#Silverpeas-Spnego

###This project is a fork of the spnego.sourceforge.net project

* Fork of [https://github.com/joval/SPNEGO](https://github.com/joval/SPNEGO) which corresponds to
  stable spnego-r7.jar (2010-OCT-15)
* Install Guide, Reference and API Documentation can be found
  at: [http://spnego.sourceforge.net](http://spnego.sourceforge.net)
* Before getting started, the pre-flight doc is a must
  read [http://spnego.sourceforge.net/pre_flight.html](http://spnego.sourceforge.net/pre_flight.html)
* Require JDK 11 or higher and Jakarta EE 10 servlet && SOAP APIs to compile source

In order to perform user authentication in our Silverpeas product by SSO using the SPNEGO
mechanism within the Kerberos protocol, we were interested in the Sourceforge Spnego project.
Despite several successful integration tests, we identified some additional requirements in
order to manage more precisely, in a Jakarta EE application such as Silverpeas, the different
possible errors that can happen during the SSO negotiation process for a user.

We then made the necessary developments and have proposed them as a contribution to the
project [https://github.com/joval/SPNEGO] (https://github.com/joval/SPNEGO).
As it has not been integrated, and after several months without any responses or feedbacks, we 
decided to make our own fork of the project that includes our needs.

###The contributions of Silverpeas's version:

* Adding apache maven building capabilities
* Migrating to Jakarta EE
* Adding typed runtime exceptions that can be used to handle SSO errors in the Jakarta EE
  application (not enabled by default, this feature can be enabled by setting the filter 
  parameter `spnego.throw.typedRuntimeException` to `true`)
* Updating the SPNEGO HTTP Filter so that it can be used in several URL matchers (filter mapping)
* Modifying the extraction of the remote user name (removing from the Kerberos Principal only the 
  part of the Kerberos REALM)