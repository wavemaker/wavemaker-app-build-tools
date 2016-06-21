package com.wavemaker.studio.app.build.exception;

/**
 * @author <a href="mailto:sunil.pulugula@wavemaker.com">Sunil Kumar</a>
 * @since 21/6/16
 */
public class ServiceDefGenerationException extends Exception {

    public ServiceDefGenerationException(String s) {
        super(s);
    }

    public ServiceDefGenerationException(String s, Throwable cause) {
        super(s, cause);
    }

    public ServiceDefGenerationException(Throwable cause) {
        super(cause);
    }
}
