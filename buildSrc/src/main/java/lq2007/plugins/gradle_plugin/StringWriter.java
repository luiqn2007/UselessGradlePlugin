package lq2007.plugins.gradle_plugin;

import java.io.*;

public class StringWriter extends Writer {

    private StringBuilder sb = new StringBuilder();

    @Override
    public void write(char[] cbuf, int off, int len) {
        sb.append(cbuf, off, len);
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {
        sb = new StringBuilder();
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
