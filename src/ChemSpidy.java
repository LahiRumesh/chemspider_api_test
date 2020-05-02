import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;



public class ChemSpidy {


    public static String APIKEY =""; // Enter your API key
    public static int HTTP_SUCCES=200;

    public static String getQueryID(String mass,String range) throws IOException {

        final String POST_PARAMS = "{\n" + "\"mass\":"+ mass+",\r\n" +
                "    \"range\":"+ range+",\r\n" +
                "    \"component\": \"any\",\r\n" +
                "    \"dataSources\": [\"ChemBridge\",\"ChemBlock\",\"ChemSpiderman\"],\r\n" +
                "    \"orderBy\": \"default\",\r\n" +
                "    \"orderDirection\": \"default\"" + "\n}";
        String queryID=null;
        URL obj = new URL("https://api.rsc.org/compounds/v1/filter/mass");
        HttpURLConnection postConnection = (HttpURLConnection) obj.openConnection();
        postConnection.setRequestMethod("POST");
        postConnection.setRequestProperty("apikey", APIKEY);
        postConnection.setRequestProperty("Content-Type", "application/json");
        postConnection.setDoOutput(true);
        OutputStream os = postConnection.getOutputStream();
        os.write(POST_PARAMS.getBytes());
        os.flush();
        os.close();
        int responseCode = postConnection.getResponseCode();
        if (responseCode == HTTP_SUCCES) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    postConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in .readLine()) != null) {
                response.append(inputLine);
            } in .close();
            queryID=response.toString();
            queryID=queryID.substring(12,48);
        } else {
            System.out.println("POST NOT WORKED");
        }
        return queryID;
    }

    public static String getResults(String queryID) throws IOException {
        String results = null;
        URL obj = new URL("https://api.rsc.org/compounds/v1/filter/"+queryID+"/results/sdf");
        HttpURLConnection getcon = (HttpURLConnection) obj.openConnection();
        getcon.setRequestMethod("GET");
        getcon.setRequestProperty("User-Agent", "Mozilla/5.0");
        getcon.setRequestProperty("apikey", APIKEY);
        getcon.setRequestProperty("Content-Type", "application/json");

        int responseCode = getcon.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) { // success
            BufferedReader in = new BufferedReader(new InputStreamReader(getcon.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {

                response.append(inputLine);
            }
            in.close();

            String data[]=(response.toString()).split(":");
            String data2[]=(data[1]).split("\"");
            results=data2[1];
        } else {
            System.out.println("GET request not worked");
        }
        return results;
    }

    public static void decompressSDF(String encoded,String pdf_name) throws Base64DecodingException, IOException {
        byte[] compressed = Base64.decode(encoded);
        ArrayList<String> ar = new ArrayList<String>();

        if ((compressed == null) || (compressed.length == 0)) {
            throw new IllegalArgumentException("Cannot unzip null or empty bytes");
        }
        if (!isZipped(compressed)) {
            System.out.println(compressed);
        }

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressed)) {
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream)) {
                try (InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8)) {
                    try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                        StringBuilder output = new StringBuilder();
                        String line;
                        while((line = bufferedReader.readLine()) != null){
                            output.append(line);
                            ar.add(line);
                        }
                    }
                }
            }
        } catch(IOException e) {
            throw new RuntimeException("Failed to unzip content", e);
        }

        FileWriter writer = new FileWriter(pdf_name);
        for(String str: ar) {
            writer.write(str + System.lineSeparator());
        }
        System.out.println("File downloaded Successfully");
        writer.close();
    }

    public static boolean isZipped(final byte[] compressed) {
        return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }

    public static void main(String args[]) throws IOException, Base64DecodingException {
        String val=getQueryID("100","10");
        decompressSDF(getResults(val),"Test3.sdf");


    }
}