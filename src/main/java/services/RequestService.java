package services;

import java.util.List;

import static spark.Spark.*;

public class RequestService {
    private RedisService r;

    public RequestService(RedisService r) {
        this.r = r;
        setupEndpoints();
    }

    private void setupEndpoints() {
        port(8080);

        get("/admin/flush", (request, response) -> {
            r.flush();
            System.out.println("Flushed redis");
            return "Flushed redis";
        });

        get("/admin/filelocations", (request, response) -> r.getFileLocations());

        get("/files/*", (request, response) -> {
            String fileName = request.pathInfo().substring(7);
            System.out.println("Reading: " + fileName);
            ValueVersion valueVersion = r.read(fileName);
            return formatValueVersion(valueVersion);
        });

        post("/files/*", (request, response) -> {
            String[] query = request.pathInfo().substring(7).split(",");
            String filename = query[0];
            int version = Integer.parseInt(query[1]);
            String value = query[2];
            System.out.println("Writing: " + filename + " with value " + value + " with version " + version);
            ValueVersion valueVersion = r.write(filename, value, version);
            return formatValueVersion(valueVersion);
        });

        put("/files/*", (request, response) -> {
            System.out.println("Modify metadata: " + request.pathInfo());

            return request.pathInfo();
        });
    }

    private String formatValueVersion(ValueVersion readValue) {
        if (readValue == null) {
            return "-1,null";
        }
        List<String> values = readValue.getValues();
        StringBuilder readResponse = new StringBuilder();
        readResponse.append(readValue.getVersion());
        for(String value: values) {
            readResponse.append(',');
            readResponse.append(value);
        }
        return readResponse.toString();
    }
}
