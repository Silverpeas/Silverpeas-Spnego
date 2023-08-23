/*
 * Copyright (C) 2014-2023 Silverpeas
 * Copyright (C) 2009 "Darwin V. Felix" <darwinfelix@users.sourceforge.net>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.silverpeas.spnego;

import jakarta.xml.soap.*;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

import javax.security.auth.login.LoginException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.PrivilegedActionException;

/**
 * This class can be used to make SOAP calls to a protected SOAP Web Service.
 * <p/>
 * <p>
 * The idea for this class is to replace code that looks like this...
 * <pre>
 *  final SOAPConnectionFactory soapConnectionFactory =
 *      SOAPConnectionFactory.newInstance();
 *  conn = soapConnectionFactory.createConnection();
 * </pre>
 * </p>
 * <p/>
 * <p>
 * with code that looks like this...
 * <pre>
 *  conn = new SpnegoSOAPConnection("spnego-client", "dfelix", "myp@s5");
 * </pre>
 * </p>
 * <p/>
 * <p><b>Example:</b></p>
 * <pre>
 * SOAPMessage response = null;
 *
 * <b>final SpnegoSOAPConnection conn =
 *     new SpnegoSOAPConnection(this.module, this.kuser, this.kpass);</b>
 *
 * try {
 *     final MessageFactory msgFactory = MessageFactory.newInstance();
 *     final SOAPMessage message = msgFactory.createMessage();
 *
 *     final SOAPBody body = message.getSOAPBody();
 *
 *     final SOAPBodyElement bodyElement = body.addBodyElement(
 *             new QName(this.namespace, this.methodName, this.nsprefix));
 *
 *     for (int i=0; i&lt;args.length; i++) {
 *         final SOAPElement element = bodyElement.addChildElement(
 *                 new QName("arg" + i));
 *
 *         element.addTextNode(args[i]);
 *     }
 *
 *     response = conn.call(message, this.serviceLocation);
 *
 * } finally {
 *     conn.close();
 * }
 * </pre>
 * <p/>
 * <p>
 * To see a full working example, take a look at the
 * <a href="http://spnego.sourceforge.net/ExampleSpnegoSOAPClient.java"
 * target="_blank">ExampleSpnegoSOAPClient.java</a>
 * example.
 * </p>
 * <p/>
 * <p>
 * Also, take a look at the
 * <a href="http://spnego.sourceforge.net/protected_soap_service.html"
 * target="_blank">how to connect to a protected SOAP Web Service</a>
 * example.
 * </p>
 * @author Darwin V. Felix
 * @see SpnegoHttpURLConnection
 */
public class SpnegoSOAPConnection extends SOAPConnection {

  private final transient SpnegoHttpURLConnection conn;

  /**
   * Creates an instance where the LoginContext relies on a keytab
   * file being specified by "java.security.auth.login.config" or
   * where LoginContext relies on the session key.
   * @param loginModuleName the name of the login module
   * @throws LoginException if the login context cannot be got.
   */
  @SuppressWarnings("UnusedDeclaration")
  public SpnegoSOAPConnection(final String loginModuleName) throws LoginException {

    super();
    this.conn = new SpnegoHttpURLConnection(loginModuleName);
  }

  /**
   * Create an instance where the GSSCredential is specified by the parameter
   * and where the GSSCredential is automatically disposed after use.
   * @param creds credentials to use
   */
  @SuppressWarnings("UnusedDeclaration")
  public SpnegoSOAPConnection(final GSSCredential creds) {
    this(creds, true);
  }

  /**
   * Create an instance where the GSSCredential is specified by the parameter
   * and whether the GSSCredential should be disposed after use.
   * @param creds credentials to use
   * @param dispose true if GSSCredential should be diposed after use
   */
  public SpnegoSOAPConnection(final GSSCredential creds, final boolean dispose) {
    super();
    this.conn = new SpnegoHttpURLConnection(creds, dispose);
  }

  /**
   * Creates an instance where the LoginContext does not require a keytab
   * file. However, the "java.security.auth.login.config" property must still
   * be set prior to instantiating this object.
   * @param loginModuleName the name of the login module
   * @param username the user login
   * @param password the user password
   * @throws LoginException if the login context cannot be got.
   */
  @SuppressWarnings("UnusedDeclaration")
  public SpnegoSOAPConnection(final String loginModuleName, final String username,
      final String password) throws LoginException {

    super();
    this.conn = new SpnegoHttpURLConnection(loginModuleName, username, password);
  }

  @Override
  public final SOAPMessage call(final SOAPMessage request, final Object endpoint)
      throws SOAPException {

    SOAPMessage message;
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();

    try {
      final MimeHeaders headers = request.getMimeHeaders();
      final String[] contentType = headers.getHeader("Content-Type");
      final String[] soapAction = headers.getHeader("SOAPAction");

      // build the Content-Type HTTP header parameter if not defined
      if (null == contentType) {
        final StringBuilder header = new StringBuilder();

        if (null == soapAction) {
          header.append("application/soap+xml; charset=UTF-8;");
        } else {
          header.append("text/xml; charset=UTF-8;");
        }

        // not defined as a MIME header but we need it as an HTTP header parameter
        this.conn.addRequestProperty("Content-Type", header.toString());
      } else {
        if (contentType.length > 1) {
          throw new IllegalArgumentException("Content-Type defined more than once.");
        }

        // user specified as a MIME header so add it as an HTTP header parameter
        this.conn.addRequestProperty("Content-Type", contentType[0]);
      }

      // specify SOAPAction as an HTTP header parameter
      if (null != soapAction) {
        if (soapAction.length > 1) {
          throw new IllegalArgumentException("SOAPAction defined more than once.");
        }
        this.conn.addRequestProperty("SOAPAction", soapAction[0]);
      }

      request.writeTo(bos);

      this.conn.connect(new URL(endpoint.toString()), bos);

      final MessageFactory factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);

      try {
        message = factory.createMessage(null, this.conn.getInputStream());
      } catch (IOException e) {
        message = factory.createMessage(null, this.conn.getErrorStream());
      }

    } catch (PrivilegedActionException | GSSException | IOException e) {
      throw new SOAPException(e);
    } finally {
      try {
        bos.close();
      } catch (IOException ioe) {
        assert true;
      }
      this.close();
    }

    return message;
  }

  @Override
  public final void close() {
    if (null != this.conn) {
      this.conn.disconnect();
    }
  }
}
