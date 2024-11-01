package ee.eki.tolkevarav.sso.keycloakserviceprovider.tokenclaimsmapper;

class FatalException extends RuntimeException {
    public FatalException(Exception exception) {
        super(exception);
    }
}
