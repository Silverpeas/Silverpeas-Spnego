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

import org.ietf.jgss.GSSCredential;
import org.silverpeas.spnego.SpnegoHttpFilter.Constants;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.security.Principal;

/**
 * Wrap ServletRequest so we can do our own handling of the
 * principal and auth types.
 * <p/>
 * <p>Also, see the documentation on the {@link DelegateServletRequest} class.</p>
 * <p/>
 * <p>Finally, a credential delegation example can be found on
 * <a href="http://spnego.sourceforge.net" target="_blank">http://spnego.sourceforge.net</a>
 * </p>
 * @author Darwin V. Felix
 */
final class SpnegoHttpServletRequest extends HttpServletRequestWrapper
    implements DelegateServletRequest {

  /**
   * Client Principal.
   */
  private final transient SpnegoPrincipal principal;

  /**
   * Creates a Servlet Request specifying KerberosPrincipal of user.
   * @param request the incoming HTTP request
   * @param spnegoPrincipal the principal behind the request.
   */
  SpnegoHttpServletRequest(final HttpServletRequest request,
      final SpnegoPrincipal spnegoPrincipal) {

    super(request);

    this.principal = spnegoPrincipal;
  }

  /**
   * Returns "Negotiate" or "Basic" else default auth type.
   * @see jakarta.servlet.http.HttpServletRequest#getAuthType()
   */
  @Override
  public String getAuthType() {

    final String authType;
    final String header = this.getHeader(Constants.AUTHZ_HEADER);

    if (header.startsWith(Constants.NEGOTIATE_HEADER)) {
      authType = Constants.NEGOTIATE_HEADER;

    } else if (header.startsWith(Constants.BASIC_HEADER)) {
      authType = Constants.BASIC_HEADER;

    } else {
      authType = super.getAuthType();
    }

    return authType;
  }

  /**
   * Return the client's/requester's delegated credential or null.
   * @return client's delegated credential or null.
   */
  public GSSCredential getDelegatedCredential() {
    return this.principal.getDelegatedCredential();
  }

  /**
   * Returns authenticated username (without domain/realm) else default username.
   * @see jakarta.servlet.http.HttpServletRequest#getRemoteUser()
   */
  @Override
  public String getRemoteUser() {

    if (null == this.principal) {
      return super.getRemoteUser();
    } else {
      // Removing the Kerberos REALM from the name to obtain the user name
      String principalName = this.principal.getName();
      int indexOfRealm = principalName.lastIndexOf("@");
      if (indexOfRealm >= 0) {
        return principalName.substring(0, indexOfRealm);
      }
      return principalName;
    }
  }

  /**
   * Returns KerberosPrincipal of user.
   * @see jakarta.servlet.http.HttpServletRequest#getUserPrincipal()
   */
  @Override
  public Principal getUserPrincipal() {
    return this.principal;
  }
}
