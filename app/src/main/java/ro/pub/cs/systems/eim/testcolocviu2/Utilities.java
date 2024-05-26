package ro.pub.cs.systems.eim.testcolocviu2;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

public class Utilities {
    public static BufferedReader getReader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public static PrintWriter getWriter(Socket socket) throws IOException {
        return new PrintWriter(socket.getOutputStream(), true);
    }

    public static double toCelsius(double kelvin) {
        return kelvin - 273.15;
    }

    public static String parseParam(String info, String param) {
        if (param.equals("all")) {
            try {
                JSONObject result = new JSONObject(info);
                JSONObject params = result.getJSONObject("main");
                return params.toString();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            JSONObject result = new JSONObject(info);
            JSONObject params = result.getJSONObject("main");
            Double paramValue = new Double(params.getDouble(param));

            if (param.equals("temp")) {
                paramValue = toCelsius(paramValue);
            }

            return paramValue.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getLink(String city) {
        return Constants.API_ADDR + "?q=" + city + "&appid=" + Constants.KEY;
    }

    public static String getUrlContent(String urlString) {
        StringBuilder result = new StringBuilder();
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            } else {
                throw new IOException("HTTP error code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return result.toString();
    }

}
