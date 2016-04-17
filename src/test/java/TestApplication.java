import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiaweizhang on 4/16/16.
 */
public class TestApplication {
    private final String FILENAME = "small.txt";


    public static void main(String args[]) {
        TestApplication app = new TestApplication();
        app.start();
    }

    private void start() {
        reset();
        List<String> lines = getLines();

        for (String line : lines) {
            String[] arr = line.split("\\s+");
            switch (arr[0]) {
                case "t":
                    try {
                        Thread.sleep(Integer.parseInt(arr[1])*1000);
                    } catch(InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    break;
                case "r":
                    readFile(arr[1]);
                    break;
                case "w":
                    writeFile(arr[1]);
                    break;
                case "f":
                    getFileLocations();
                    break;
                default:
                    System.out.println("Unrecognized code: "+ arr[0]);
                    break;
            }
        }
        System.out.println("Test finished");
    }

    private List<String> getLines() {
        String directory = "src/test/resources/" + FILENAME;
        //String directory = "/src/main/test/resources/" + "large.txt";

        List<String> lines = new ArrayList<String>();
        try(BufferedReader br = new BufferedReader(new FileReader(directory))) {
            String line = br.readLine();

            while (line != null) {
                lines.add(line);
                line = br.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lines;
    }

    private void writeFile(String name) {
        HttpResponse<String> result = null;
        try {
            result = Unirest.post("http://localhost:8080/files/" + name)
                    .asString();
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        String str = result.getBody();
        System.out.println(str);
    }

    private void readFile(String name) {
        HttpResponse<String> result = null;
        try {
            result = Unirest.get("http://localhost:8080/files/" + name)
                    .asString();
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        String str = result.getBody();
        System.out.println(str);
    }

    private void getFileLocations() {
        HttpResponse<String> result = null;
        try {
            result = Unirest.get("http://localhost:8080/admin/filelocations")
                    .asString();
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        String str = result.getBody();
        System.out.println();
        System.out.println(str);
        System.out.println();
    }

    private void reset() {
        HttpResponse<String> result = null;
        try {
            result = Unirest.get("http://localhost:8080/admin/flush")
                    .asString();
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        String str = result.getBody();
        System.out.println(str);
    }
}
