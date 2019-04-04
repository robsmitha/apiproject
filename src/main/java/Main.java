import com.taxjar.Taxjar;
import com.taxjar.exception.TaxjarException;
import com.taxjar.model.categories.Category;
import com.taxjar.model.categories.CategoryResponse;
import com.taxjar.model.rates.Rate;
import com.taxjar.model.rates.RateResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.json.*;

public class Main {
    private static Taxjar client;
    private static HttpURLConnection con;
    private static String Google_Api_Endpoint = "https://maps.googleapis.com/maps/api/geocode/json?address=";
    private static String Google_Api_Key = "YOUR_API_KEY_HERE";
    private static String TaxJar_Api_Key = "YOUR_API_KEY_HERE";

    public static void main(String[] args){
        RunProgram();
    }

    private static String GoogleMapsApiCall(String address){
        try
        {
            return Google_Api_Endpoint + URLEncoder.encode(address, "UTF-8") + "&key=" + Google_Api_Key;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static void RunProgram(){

        System.out.println("Tax Look Up Program v1.0.0.0");
        System.out.println("-------------------------------------------");

        client = new Taxjar(TaxJar_Api_Key);

        System.out.println("Enter an address for the local tax rate: ");
        Scanner scanner = new Scanner(System.in);  // Create a Scanner object
        String address = scanner.nextLine();
        String googleMapsApiCall = GoogleMapsApiCall(address);
        try{
            assert googleMapsApiCall != null;
            URL urlObj = new URL(googleMapsApiCall);
            con = (HttpURLConnection) urlObj.openConnection();
            con.setRequestMethod("GET");
            StringBuilder content;

            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {

                String line;
                content = new StringBuilder();

                while ((line = in.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }

            JSONObject obj = new JSONObject(content.toString());
            String status = obj.getString("status");
            if(status.toUpperCase().equals("OK")){
                JSONArray results = obj.getJSONArray("results");
                JSONObject result = results.getJSONObject(0);
                String formatted_address = result.getString("formatted_address");
                JSONArray address_components = result.getJSONArray("address_components");
                String zipcode = "";
                String city = "";
                String country = "";
                for(int i = 0; i < address_components.length(); i++){
                    JSONObject component = address_components.getJSONObject(i);
                    JSONArray types = component.getJSONArray("types");
                    String type = "";
                    for(int j = 0; j < types.length(); j++){
                        type = types.getString(j);
                        switch (type){
                            case "locality":
                                city = component.getString("long_name");
                                break;
                            case "country":
                                country = component.getString("short_name");
                                break;
                            case "postal_code":
                                zipcode = component.getString("short_name");
                                break;
                            //TODO: implement support for remaining components
                        }
                    }
                }
                System.out.println("-------------------------------------------");
                System.out.println("\nGoogle API response: \n" + formatted_address);
                Rate rate = GetRate(country, city, zipcode);
                if(rate != null){
                    System.out.println("\nTaxJar API response:");
                    System.out.println("County: " + rate.getCounty() + " - Tax Rate: " + rate.getCountyRate());
                    System.out.println("State: " +rate.getState() + " - Tax Rate: " + rate.getStateRate());
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            con.disconnect();
        }
    }
    private static Rate GetRate(String country, String city, String zipcode){
        try {
            Map<String, String> params = new HashMap<>();
            params.put("country",  country);
            params.put("city", city);
            RateResponse res = client.ratesForLocation(zipcode, params);
            return res.rate;
        } catch (TaxjarException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static CategoryResponse GetCategories(){
        try {
            return client.categories();
        } catch (TaxjarException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void PrintCategories(CategoryResponse res){
        if(res == null){
            return;
        }
        for (Category r :
                res.categories ) {
            System.out.println(r.getName() + " - " + r.getDescription());
            System.out.println(r.getProductTaxCode());
            System.out.println("--------------------------------------");
        }
    }

}
