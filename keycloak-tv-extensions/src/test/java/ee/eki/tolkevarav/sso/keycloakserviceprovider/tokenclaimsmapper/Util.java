package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.utils.URIUtils;
import org.apache.hc.core5.net.URIAuthority;
import org.testcontainers.containers.Container;

import java.net.URI;
import java.util.Map;


public class Util {
    static Map<String, Object> convertToMap(Object input)  {
        try {
            return new ObjectMapper().readValue(input.toString(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    static URIAuthority buildContainerUriAuthority(Container<?> container, int port) {
            return new URIAuthority(container.getHost(), container.getMappedPort(port));
    }

    static CloseableHttpClient buildHttpClient() {
        return HttpClients.custom()
            .disableRedirectHandling()
            .build();
    }

    static boolean isSameHostName(URI thisUri, URI thatUri) {
        if (thisUri == null || thatUri == null) {
            return false;
        }

        return URIUtils.extractHost(thisUri).getHostName().equals(
            URIUtils.extractHost(thatUri).getHostName()
        );
    }

    static boolean isSameHostName(URI thisUri, String thatUri) {
        if (thisUri == null || thatUri == null) {
            return false;
        }

        return URIUtils.extractHost(thisUri).getHostName().equals(thatUri);
    }
}
