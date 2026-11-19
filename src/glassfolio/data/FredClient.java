package glassfolio.data;

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
 * Based on a FRED request example by Sam Greenwell. FRED answers in XML,
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
     * @param apiKey the FRED API key, from {@link ApiKeyStore}. Never hardcode it.
     * @throws IllegalArgumentException if the key is missing or not in FRED's format
     */
    public FredClient(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Missing FRED API key.");
        }
        if (!ApiKeyStore.isValidFormat(apiKey)) {
            throw new IllegalArgumentException("A FRED API key is 32 letters and numbers.");
        }
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    @Override
    public List<Double> getValues(String seriesId, LocalDate startDate, String units)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(buildUrl(seriesId, startDate, units))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        HttpResponse<byte[]> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) { // no connection, DNS failure or timeout
            throw new IOException("Couldn't reach FRED. Check your internet connection and try again.", e);
        }
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
     * @param units     FRED's units code, for example "lin"
     * @return the full request URL
     */
    private URI buildUrl(String seriesId, LocalDate startDate, String units) {
        StringBuilder url = new StringBuilder(BASE_URL)
                .append("?series_id=").append(URLEncoder.encode(seriesId, StandardCharsets.UTF_8))
                .append("&api_key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8))
                .append("&observation_start=").append(startDate)
                .append("&units=").append(URLEncoder.encode(units, StandardCharsets.UTF_8));
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
                double parsed;
                try {
                    parsed = Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    throw new IOException("FRED sent a value that is not a number", e);
                }
                if (!Double.isFinite(parsed)) { // parseDouble also accepts "NaN" and "Infinity"
                    throw new IOException("FRED sent a value that is not a number");
                }
                values.add(parsed);
            }
        }
        return values;
    }
}
