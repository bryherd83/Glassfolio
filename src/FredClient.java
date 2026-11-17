import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Downloads data series from FRED (Federal Reserve Economic Data).
 * Based on Professor Greenwell's request example. FRED answers in XML,
 * which Java reads with its built-in parser, so no extra library is needed.
 */
public class FredClient implements MarketDataSource {
    private static final String BASE_URL = "https://api.stlouisfed.org/fred/series/observations";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int HTTP_OK = 200;
    private static final String MISSING_VALUE = "."; // FRED's marker for days with no data, like holidays
    private static final String DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";

    private final HttpClient http;
    private final String apiKey;

    /**
     * Creates a client that uses the given API key.
     *
     * @param apiKey the FRED API key. Read it from an environment variable, never hardcode it.
     * @throws IllegalArgumentException if the key is missing or blank
     */
    public FredClient(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Missing FRED API key. Set the FRED_API_KEY environment variable.");
        }
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    @Override
    public List<Double> getValues(String seriesId, LocalDate startDate) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(buildUrl(seriesId, startDate))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != HTTP_OK) {
            throw new IOException("FRED request for " + seriesId + " failed with status " + response.statusCode()
                    + " (400 usually means a bad API key)");
        }
        return parseValues(response.body());
    }

    /**
     * Builds the request URL with a StringBuilder (Builder pattern).
     *
     * @param seriesId  the series id
     * @param startDate the first day to include
     * @return the full request URL
     */
    private URI buildUrl(String seriesId, LocalDate startDate) {
        StringBuilder url = new StringBuilder(BASE_URL)
                .append("?series_id=").append(URLEncoder.encode(seriesId, StandardCharsets.UTF_8))
                .append("&api_key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8))
                .append("&observation_start=").append(startDate);
        return URI.create(url.toString());
    }

    /**
     * Reads the observation values out of FRED's XML answer.
     *
     * @param xml the raw response body
     * @return the values, oldest first, without missing days
     * @throws IOException if the XML can't be read
     */
    private List<Double> parseValues(byte[] xml) throws IOException {
        Document doc;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Block DOCTYPEs so a bad response can't make the parser read local files (XXE)
            factory.setFeature(DISALLOW_DOCTYPE, true);
            doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("FRED sent data that could not be read", e);
        }

        NodeList observations = doc.getElementsByTagName("observation");
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < observations.getLength(); i++) {
            String value = ((Element) observations.item(i)).getAttribute("value");
            if (!value.equals(MISSING_VALUE)) {
                values.add(Double.parseDouble(value));
            }
        }
        return values;
    }
}
