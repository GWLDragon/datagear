package org.datagear.web.util;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author gwl
 * @date 2020/11/5 17:58
 * @since 1.0.0
 */

public class ResponseWrapper extends HttpServletResponseWrapper {

    private ByteArrayOutputStream buffer;

    private ServletOutputStream out;

    public ResponseWrapper(HttpServletResponse httpServletResponse)
    {
        super(httpServletResponse);
        buffer = new ByteArrayOutputStream();
        out = new WrapperOutputStream(buffer);
    }

    @Override
    public ServletOutputStream getOutputStream()
            throws IOException
    {
        return out;
    }

    @Override
    public void flushBuffer()
            throws IOException
    {
        if (out != null)
        {
            out.flush();
        }
    }

    public byte[] getContent()
            throws IOException
    {
        flushBuffer();
        return buffer.toByteArray();
    }

    class WrapperOutputStream extends ServletOutputStream
    {
        private ByteArrayOutputStream bos;

        public WrapperOutputStream(ByteArrayOutputStream bos)
        {
            this.bos = bos;
        }

        @Override
        public void write(int b)
                throws IOException
        {
            bos.write(b);
        }



    }

}
