import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Date;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, ParseException {

        final String CITY = "Saint%20Petersburg";
        final String API_KEY = "c2517eeb640c867f6166aa3a8823edac";
        final String TIMES_OF_DAY = "night";
        final int DAY_COUNT = 5;
        double lon;
        double lat;
        DateFormat format = new SimpleDateFormat("yyyy.MM.dd");
        DecimalFormat decimalFormat = new DecimalFormat("##.##");
        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build();

        //Get geo coordination
        String uriGeoCoding = "http://api.openweathermap.org/data/2.5/forecast?q=" + CITY + "&appid="+ API_KEY;
        JSONObject jsonGeo = getJsonResponse(httpClient, uriGeoCoding);

        JSONObject city = (JSONObject) jsonGeo.get("city");
        JSONObject coord = city.getJSONObject("coord");
        lon = coord.getDouble("lon");
        lat = coord.getDouble("lat");

        //Get weather response
        String uri = "https://api.openweathermap.org/data/2.5/onecall?lat=" + lat + "&lon=" + lon + "&exclude=minutely&appid=" + API_KEY + "&units=metric";
        JSONObject jsonObject = getJsonResponse(httpClient, uri);
        JSONArray list = (JSONArray) jsonObject.get("daily");

        //Parse response
        long date = 0;
        long maxDayLightDate = 0;
        double differenceActualAndFellLike = Double.MAX_VALUE;
        int differenceSunriseAndSunset = Integer.MIN_VALUE;

        for (int i = 0; i < (Math.min(list.length(), DAY_COUNT)) ; i++) {
            JSONObject currentObject = (JSONObject) list.get(i);
            long currentTime = currentObject.getLong("dt");


            JSONObject temp = (JSONObject) currentObject.get("temp");
            JSONObject feelsLike = (JSONObject) currentObject.get("feels_like");

            double currentActualAndFeels = temp.getDouble(TIMES_OF_DAY) - feelsLike.getDouble(TIMES_OF_DAY);
            if (currentActualAndFeels < differenceActualAndFellLike) {
                differenceActualAndFellLike = currentActualAndFeels;
                date = currentTime;
            }

            int currentSunsetAndSunrise = currentObject.getInt("sunset") - currentObject.getInt("sunrise");
            if (currentSunsetAndSunrise > differenceSunriseAndSunset){
                differenceSunriseAndSunset = currentSunsetAndSunrise;
                maxDayLightDate = currentTime;
            }
        }

        System.out.println("The night with the smallest difference between feel and actual temperature is expected on "
                + format.format(new Date(date * 1000)) + " and will be " + decimalFormat.format(differenceActualAndFellLike) + "℃");

        System.out.println("The longest day is " + format.format(new Date(maxDayLightDate * 1000)) + " and lasts "
                + LocalTime.ofSecondOfDay(differenceSunriseAndSunset));

    }

    private static JSONObject getJsonResponse (HttpClient client, String uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(10))
                .GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return new JSONObject(response.body());
    }
}
