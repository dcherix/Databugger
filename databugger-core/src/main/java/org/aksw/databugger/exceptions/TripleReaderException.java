package org.aksw.databugger.exceptions;

/**
 * User: Dimitris Kontokostas
 * Exceptions for readers that cannot read()
 * Created: 11/14/13 8:35 AM
 */
public class TripleReaderException extends Exception {


    public TripleReaderException() {
    }

    public TripleReaderException(String message, Throwable e) {
        super(message, e);
    }

    public TripleReaderException(String message) {
        super(message);
    }

    public TripleReaderException(Throwable e) {
        super(e);
    }

    public TripleReaderException(Exception e) {
        super(e);
    }
}