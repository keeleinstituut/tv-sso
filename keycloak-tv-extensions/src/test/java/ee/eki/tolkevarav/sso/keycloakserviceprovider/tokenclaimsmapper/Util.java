package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
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
        CookieStore baseStore = new BasicCookieStore();
        // Keycloak 26 requires cookies to be maintained during authentication flows.
        // However, Keycloak sets cookies with the Secure flag, which prevents HttpClient
        // from sending them over HTTP connections (used in tests). We create a wrapper
        // cookie store that strips the Secure flag from cookies for test compatibility.
        CookieStore cookieStore = new CookieStore() {
            @Override
            public void addCookie(Cookie cookie) {
                // Create a copy without Secure flag for HTTP connections in tests
                BasicClientCookie insecureCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
                insecureCookie.setDomain(cookie.getDomain());
                insecureCookie.setPath(cookie.getPath());
                insecureCookie.setExpiryDate(cookie.getExpiryDate());
                insecureCookie.setSecure(false); // Remove Secure flag for HTTP
                if (cookie.getAttribute("SameSite") != null) {
                    insecureCookie.setAttribute("SameSite", cookie.getAttribute("SameSite"));
                }
                baseStore.addCookie(insecureCookie);
            }

            @Override
            public java.util.List<Cookie> getCookies() {
                return baseStore.getCookies();
            }

            @Override
            public boolean clearExpired(java.util.Date date) {
                return baseStore.clearExpired(date);
            }

            @Override
            public void clear() {
                baseStore.clear();
            }
        };
        return HttpClients.custom()
            .setDefaultCookieStore(cookieStore)
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
