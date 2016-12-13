import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Client {

    private int id;
    private static final String SERVER_URL = "http://localhost:8080/";
    private static final boolean PRINT = true;

    public Client(int id){
        this.id = id;
    }

    private class Data {
        private int version;
        private List<String> values;

        public Data(String response){
            String[] parts = response.split(",");
            this.version = Integer.parseInt(parts[0].trim());
            this.values = new ArrayList<>();
            for(int i = 1; i < parts.length; i++){
                values.add(parts[i].trim());
            }
        }

        public int getVersion(){
            return version;
        }

        public List<String> getValues(){
            return values;
        }

        public String toString(){
            return "{version: " + version + ", values: " + values + "}";
        }
    }

    private String getRequest(String filename){
        try{
            String urlString = SERVER_URL + "files/" + filename;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.getResponseCode();
            conn.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        }
        catch (Exception e){
            System.out.println(e);
            return null;
        }
    }

    private String postRequest(String filename, String value, int version){
        try{
            String urlString = SERVER_URL + "files/?" + "version=" + version + "&value=" + value;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            conn.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();

        }
        catch (Exception e){
            System.out.println(e);
            return null;
        }
    }

    private void print(String s){
        if(PRINT){
            System.out.println("Client id " + id + " : ");
        }
    }

    private Data reconcile(String filename, Data data){
        if(data.getValues().size() > 1){
            print("Reconciling " + data.toString());
            String rec = data.getValues().get((data.getValues().size()-1)%id);
            Data newData = new Data(postRequest(filename, rec, data.getVersion()+1));
            return reconcile(filename, newData);
        }
        else{
            return data;
        }
    }

    public String read(String filename){
        String response = getRequest(filename);
        Data data = new Data(response);
        print("Read " + data.toString());
        return reconcile(filename, data).toString();
    }

    public String write(String filename, String value){
        return this.write(filename, value, (new Data(getRequest(filename))).getVersion());
    }

    public String write(String filename, String value, int version){
        Data data = new Data(this.postRequest(filename, value, version));
        print("Write " + data.toString());
        return data.toString();
    }
}
