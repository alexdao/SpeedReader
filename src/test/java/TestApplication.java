import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * Created by jiaweizhang on 4/16/16.
 */
public class TestApplication {
    public static void main(String args[]) {
        TestApplication app = new TestApplication();
        app.start();
    }

    private void start() {
        writeFile("file1");
        writeFile("file2");
        readFile("file1");
        readFile("file2");
        readFile("file1");
        readFile("file1");
        readFile("file1");
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
        System.out.println("\nTesting write: ");
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
        System.out.println("\nTesting read: ");
        System.out.println(str);
    }
}
