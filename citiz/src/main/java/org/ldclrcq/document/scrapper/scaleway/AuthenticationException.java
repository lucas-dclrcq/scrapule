package org.ldclrcq.document.scrapper.scaleway;

import java.io.IOException;

public class AuthenticationException extends Exception {
    public AuthenticationException(Exception e) {
        super(e);
    }
}
