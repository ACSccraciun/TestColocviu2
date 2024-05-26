package ro.pub.cs.systems.eim.testcolocviu2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private EditText serverPortTxt;
    private EditText clientPortTxt;
    private EditText clientAddrTxt;

    private TextView resultTxt;
    private Spinner paramSpinner;

    private Button serverCreateBtn;
    private Button clientGetBtn;

    private EditText cityTxt;


    public int PORT;
    public String ADDR;

    public String PARAM;

    ServerThread server;


    private class CommunicationThread extends Thread {
        private Socket socket;

        private String city;
        private String param;

        private HashMap<String, String> cache;
        public CommunicationThread(Socket socket, String city, String param, HashMap<String, String> cache) {
            this.socket = socket;
            this.city = city;
            this.param = param;
            this.cache = cache;
        }

        public String getParam() {
            if (cache.containsKey(city)) {
                return Utilities.parseParam(cache.get(city), param);
            }

            String response = Utilities.getUrlContent(Utilities.getLink(city));

            cache.put(city, response);
            return Utilities.parseParam(cache.get(city), param);
        }

        @Override
        public void run() {
            try {
                Log.v(Constants.TAG, "Connection opened with "+socket.getInetAddress()+":"+socket.getLocalPort());
                PrintWriter printWriter = Utilities.getWriter(socket);

                printWriter.println(getParam());
                socket.close();
                Log.v(Constants.TAG, "Connection closed");
            } catch (IOException ioException) {
                Log.e(Constants.TAG, "An exception has occurred: "+ioException.getMessage());
                if (Constants.DEBUG) {
                    ioException.printStackTrace();
                }
            }
        }
    }

    private class ServerThread extends Thread {
        private boolean isRunning;

        private ServerSocket serverSocket;

        private HashMap<String, String> cache;

        public void startServer() {
            cache = new HashMap<>();
            isRunning = true;
            start();
        }

        public void stopServer() {
            isRunning = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (serverSocket != null) {
                            serverSocket.close();
                        }
                        Log.v(Constants.TAG, "stopServer() method invoked "+serverSocket);
                    } catch(IOException ioException) {
                        Log.e(Constants.TAG, "An exception has occurred: "+ioException.getMessage());
                        if (Constants.DEBUG) {
                            ioException.printStackTrace();
                        }
                    }
                }
            }).start();
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(PORT);
                while (isRunning) {
                    Socket socket = serverSocket.accept();

                    BufferedReader reader = Utilities.getReader(socket);
                    String city = reader.readLine();
                    String param = reader.readLine();

                    new CommunicationThread(socket, city, param, cache).start();
                }
            } catch (IOException ioException) {
                Log.e(Constants.TAG, "An exception has occurred: "+ioException.getMessage());
                if (Constants.DEBUG) {
                    ioException.printStackTrace();
                }
            }
        }
    }

    private class ClientAsyncTask extends AsyncTask<Void, Void, String> {

        private String addr;
        private int port;

        private String city;
        private String param;

        String response;

        public ClientAsyncTask(String addr, int port, String city, String param) {
            this.addr = addr;
            this.port = port;

            this.city = city;
            this.param = param;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                Socket socket = new Socket(addr, port);
                PrintWriter writer = Utilities.getWriter(socket);
                writer.println(city);
                writer.println(param);

                BufferedReader reader = Utilities.getReader(socket);
                response = reader.readLine();

                Log.d(Constants.TAG, "The server returned: " + response);
            } catch (UnknownHostException unknownHostException) {
                Log.d(Constants.TAG, unknownHostException.getMessage());
                if (Constants.DEBUG) {
                    unknownHostException.printStackTrace();
                }
            } catch (IOException ioException) {
                Log.d(Constants.TAG, ioException.getMessage());
                if (Constants.DEBUG) {
                    ioException.printStackTrace();
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            resultTxt.setText(result);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serverPortTxt = findViewById(R.id.serverPortTxt);
        clientPortTxt = findViewById(R.id.clientPortTxt);
        clientAddrTxt = findViewById(R.id.addrTxt);
        resultTxt = findViewById(R.id.resultTxt);
        paramSpinner = findViewById(R.id.paramSpin);
        serverCreateBtn = findViewById(R.id.serverCreatebtn);
        clientGetBtn = findViewById(R.id.clientbtn);
        cityTxt = findViewById(R.id.cityTxt);

        serverCreateBtn.setOnClickListener(v -> {
            PORT = Integer.parseInt(serverPortTxt.getText().toString());
            server = new ServerThread();
            server.startServer();
        });

        clientGetBtn.setOnClickListener(v -> {
            ADDR = clientAddrTxt.getText().toString();

            String city = cityTxt.getText().toString();
            String param = paramSpinner.getSelectedItem().toString();

            ClientAsyncTask client = new ClientAsyncTask(ADDR, PORT, city, param);
            client.execute();

        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        server.stopServer();
    }
}