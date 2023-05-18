package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

class UnsuccessfulResponseException extends Exception {
    private final int statusCode;

    public UnsuccessfulResponseException(int statusCode) {

        this.statusCode = statusCode;
    }

    int getStatusCode() {
        return statusCode;
    }
}
