package org.jivesoftware.smack;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import org.apache.harmony.javax.security.auth.callback.Callback;
import org.apache.harmony.javax.security.auth.callback.CallbackHandler;
import org.apache.harmony.javax.security.auth.callback.PasswordCallback;

import android.os.Build;
import org.jivesoftware.smackx.sm.Enable;
import org.jivesoftware.smackx.sm.Resume;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Creates a socket connection to a XMPP server. This is the default connection
 * to a Jabber server and is specified in the XMPP Core (RFC 3920).
 * 
 * @see Connection
 * @author Matt Tucker
 */
public class XMPPConnection extends Connection {
	private String softVersion;
	private String softName;
	private String softOs = "Android " + Build.VERSION.RELEASE + " @ " + Build.MANUFACTURER + " " + Build.MODEL;
	private List<String> featuresList = new ArrayList<String>();
	
	public void addFeature(String var) { featuresList.add(var); }
	public List<String> getFeatures() { return featuresList; }
	public void setSoftName(String name) { this.softName = name; }
	public void setSoftVersion(String version) { this.softVersion = version; }
	public String getSoftName() { return softName; }
	public String getSoftVersion() { return softVersion; }
	public String getSoftOs() { return softOs; }

    protected Socket socket;

    String connectionID = null;
    private String user = null;
    private boolean connected = false;
    private boolean authenticated = false;
    /**
     * Flag that indicates if the user was authenticated with the server when the connection
     * to the server was closed (abruptly or not).
     */
    private boolean wasAuthenticated = false;
    private boolean anonymous = false;
    private boolean usingTLS = false;

    PacketWriter packetWriter;
    PacketReader packetReader;

    Roster roster = null;

    /**
     * Collection of available stream compression methods offered by the server.
     */
    private Collection<String> compressionMethods;
    private boolean usingCompression;


    /**
     * Creates a new connection to the specified XMPP server. A DNS SRV lookup will be
     * performed to determine the IP address and port corresponding to the
     * service name; if that lookup fails, it's assumed that server resides at
     * <tt>serviceName</tt> with the default port of 5222. Encrypted connections (TLS)
     * will be used if available, stream compression is disabled, and standard SASL
     * mechanisms will be used for authentication.<p>
     * <p/>
     * This is the simplest constructor for connecting to an XMPP server. Alternatively,
     * you can get fine-grained control over connection settings using the
     * {@link #XMPPConnection(ConnectionConfiguration)} constructor.<p>
     * <p/>
     * Note that XMPPConnection constructors do not establish a connection to the server
     * and you must call {@link #connect()}.<p>
     * <p/>
     * The CallbackHandler will only be used if the connection requires the client provide
     * an SSL certificate to the server. The CallbackHandler must handle the PasswordCallback
     * to prompt for a password to unlock the keystore containing the SSL certificate.
     *
     * @param serviceName the name of the XMPP server to connect to; e.g. <tt>example.com</tt>.
     * @param callbackHandler the CallbackHandler used to prompt for the password to the keystore.
     */
    public XMPPConnection(String serviceName, CallbackHandler callbackHandler) {
        // Create the configuration for this new connection
        super(new ConnectionConfiguration(serviceName));
        config.setCompressionEnabled(false);
        config.setSASLAuthenticationEnabled(true);
        config.setDebuggerEnabled(DEBUG_ENABLED);
        config.setCallbackHandler(callbackHandler);
    }

    /**
     * Creates a new XMPP conection in the same way {@link #XMPPConnection(String,CallbackHandler)} does, but
     * with no callback handler for password prompting of the keystore.  This will work
     * in most cases, provided the client is not required to provide a certificate to 
     * the server.
     *
     * @param serviceName the name of the XMPP server to connect to; e.g. <tt>example.com</tt>.
     */
    public XMPPConnection(String serviceName) {
        // Create the configuration for this new connection
        super(new ConnectionConfiguration(serviceName));
        config.setCompressionEnabled(false);
        config.setSASLAuthenticationEnabled(true);
        config.setDebuggerEnabled(DEBUG_ENABLED);
    }

    /**
     * Creates a new XMPP conection in the same way {@link #XMPPConnection(ConnectionConfiguration,CallbackHandler)} does, but
     * with no callback handler for password prompting of the keystore.  This will work
     * in most cases, provided the client is not required to provide a certificate to 
     * the server.
     *
     *
     * @param config the connection configuration.
     */
    public XMPPConnection(ConnectionConfiguration config) {
        super(config);
    }

    /**
     * Creates a new XMPP connection using the specified connection configuration.<p>
     * <p/>
     * Manually specifying connection configuration information is suitable for
     * advanced users of the API. In many cases, using the
     * {@link #XMPPConnection(String)} constructor is a better approach.<p>
     * <p/>
     * Note that XMPPConnection constructors do not establish a connection to the server
     * and you must call {@link #connect()}.<p>
     * <p/>
     *
     * The CallbackHandler will only be used if the connection requires the client provide
     * an SSL certificate to the server. The CallbackHandler must handle the PasswordCallback
     * to prompt for a password to unlock the keystore containing the SSL certificate.
     *
     * @param config the connection configuration.
     * @param callbackHandler the CallbackHandler used to prompt for the password to the keystore.
     */
    public XMPPConnection(ConnectionConfiguration config, CallbackHandler callbackHandler) {
        super(config);
        config.setCallbackHandler(callbackHandler);
    }

    public String getConnectionID() {
        if (!isConnected()) {
            return null;
        }
        return connectionID;
    }

    public String getUser() {
        if (!isAuthenticated()) {
            return null;
        }
        return user;
    }

    /**
     * Logs in to the server using the strongest authentication mode supported by
     * the server. If the server supports SASL authentication then the user will be
     * authenticated using SASL if not Non-SASL authentication will be tried. If more than
     * five seconds (default timeout) elapses in each step of the authentication process
     * without a response from the server, or if an error occurs, a XMPPException will be
     * thrown.<p>
     * 
     * Before logging in (i.e. authenticate) to the server the connection must be connected.
     * For compatibility and easiness of use the connection will automatically connect to the
     * server if not already connected.<p>
     *
     * It is possible to log in without sending an initial available presence by using
     * {@link ConnectionConfiguration#setSendPresence(boolean)}. If this connection is
     * not interested in loading its roster upon login then use
     * {@link ConnectionConfiguration#setRosterLoadedAtLogin(boolean)}.
     * Finally, if you want to not pass a password and instead use a more advanced mechanism
     * while using SASL then you may be interested in using
     * {@link ConnectionConfiguration#setCallbackHandler(javax.security.auth.callback.CallbackHandler)}.
     * For more advanced login settings see {@link ConnectionConfiguration}.
     *
     * @param username the username.
     * @param password the password or <tt>null</tt> if using a CallbackHandler.
     * @param resource the resource.
     * @throws XMPPException if an error occurs.
     * @throws IllegalStateException if not connected to the server, or already logged in
     *      to the serrver.
     */
    public synchronized void login(String username, String password, String resource) throws XMPPException {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to server.");
        }
        if (authenticated) {
            throw new IllegalStateException("Already logged in to server.");
        }
        // Do partial version of nameprep on the username.
        username = username.toLowerCase().trim();

        // If compression is enabled then request the server to use stream compression
        if (config.isCompressionEnabled()) {
            useCompression();
        }

        String response;
        if (config.isSASLAuthenticationEnabled() && saslAuthentication.hasNonAnonymousAuthentication()) {
            // Authenticate using SASL
            if (password != null) {
                response = saslAuthentication.authenticate(username, password, resource);
            }
            else {
                response = saslAuthentication.authenticate(username, resource, config.getCallbackHandler());
            }
        }
        else {
            // Authenticate using Non-SASL
            response = new NonSASLAuthentication(this).authenticate(username, password, resource);
        }

        // Set the user.
        if (response != null) {
            this.user = response;
            // Update the serviceName with the one returned by the server
            config.setServiceName(StringUtils.parseServer(response));
        }
        else {
            this.user = username + "@" + getServiceName();
            if (resource != null) {
                this.user += "/" + resource;
            }
        }

        if (config.isSmEnabled()) {
            if (!config.isSmResume()) {
                Enable enable = new org.jivesoftware.smackx.sm.Enable();
                enable.setResume(true);
                enable.setMax(config.getSmMax());
                sendPacket(enable);

                // Create the roster if it is not a reconnection.
                if (this.roster == null) {
                    if (rosterStorage == null) this.roster = new Roster(this);
                    else this.roster = new Roster(this,rosterStorage);
                }

                if (config.isRosterLoadedAtLogin()) {
                    this.roster.reload();
                }
            } else {
                Resume resume = new org.jivesoftware.smackx.sm.Resume();
                resume.setH(config.getSmInH());
                if (config.getSmPrevId() != null) resume.setPrevid(config.getSmPrevId());
                sendPacket(resume);
            }
        } else {
            // Create the roster if it is not a reconnection.
            if (this.roster == null) {
                if (rosterStorage == null) this.roster = new Roster(this);
                else this.roster = new Roster(this,rosterStorage);
            }

            if (config.isRosterLoadedAtLogin()) {
                this.roster.reload();
            }
        }

        // Set presence to online.
        if (config.isSendPresence()) {
            packetWriter.sendPacket(new Presence(Presence.Type.available));
        }

        // Indicate that we're now authenticated.
        authenticated = true;
        anonymous = false;

        // Stores the autentication for future reconnection
        config.setLoginInfo(username, password, resource);

        // If debugging is enabled, change the the debug window title to include the
        // name we are now logged-in as.
        // If DEBUG_ENABLED was set to true AFTER the connection was created the debugger
        // will be null
        if (config.isDebuggerEnabled() && debugger != null) {
            debugger.userHasLogged(user);
        }
    }

    /**
     * Logs in to the server anonymously. Very few servers are configured to support anonymous
     * authentication, so it's fairly likely logging in anonymously will fail. If anonymous login
     * does succeed, your XMPP address will likely be in the form "server/123ABC" (where "123ABC"
     * is a random value generated by the server).
     *
     * @throws XMPPException if an error occurs or anonymous logins are not supported by the server.
     * @throws IllegalStateException if not connected to the server, or already logged in
     *      to the serrver.
     */
    public synchronized void loginAnonymously() throws XMPPException {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to server.");
        }
        if (authenticated) {
            throw new IllegalStateException("Already logged in to server.");
        }

        String response;
        if (config.isSASLAuthenticationEnabled() &&
                saslAuthentication.hasAnonymousAuthentication()) {
            response = saslAuthentication.authenticateAnonymously();
        }
        else {
            // Authenticate using Non-SASL
            response = new NonSASLAuthentication(this).authenticateAnonymously();
        }

        // Set the user value.
        this.user = response;
        // Update the serviceName with the one returned by the server
        config.setServiceName(StringUtils.parseServer(response));

        // If compression is enabled then request the server to use stream compression
        if (config.isCompressionEnabled()) {
            useCompression();
        }

        // Anonymous users can't have a roster.
        roster = null;

        // Set presence to online.
        packetWriter.sendPacket(new Presence(Presence.Type.available));

        // Indicate that we're now authenticated.
        authenticated = true;
        anonymous = true;

        // If debugging is enabled, change the the debug window title to include the
        // name we are now logged-in as.
        // If DEBUG_ENABLED was set to true AFTER the connection was created the debugger
        // will be null
        if (config.isDebuggerEnabled() && debugger != null) {
            debugger.userHasLogged(user);
        }
    }

    public Roster getRoster() {
        if (roster == null) {
            return null;
        }
        return roster;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isSecureConnection() {
        return isUsingTLS();
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    /**
     * Closes the connection by setting presence to unavailable then closing the stream to
     * the XMPP server. The shutdown logic will be used during a planned disconnection or when
     * dealing with an unexpected disconnection. Unlike {@link #disconnect()} the connection's
     * packet reader, packet writer, and {@link Roster} will not be removed; thus
     * connection's state is kept.
     *
     * @param unavailablePresence the presence packet to send during shutdown.
     */
    protected void shutdown(Presence unavailablePresence) {
        // Set presence to offline.
    	if(packetWriter!=null){
    		packetWriter.sendPacket(unavailablePresence);
    	}

        this.setWasAuthenticated(authenticated);
        authenticated = false;
        connected = false;
        
        if(packetReader!=null){
        	packetReader.shutdown();
        }
        if(packetWriter!=null){
        	packetWriter.shutdown();
        }
        // Wait 150 ms for processes to clean-up, then shutdown.
        try {
            Thread.sleep(150);
        }
        catch (Exception e) {
            // Ignore.
        }

        // Close down the readers and writers.
        if (reader != null) {
            try {
                reader.close();
            }
            catch (Throwable ignore) { /* ignore */ }
            reader = null;
        }
        if (writer != null) {
            try {
                writer.close();
            }
            catch (Throwable ignore) { /* ignore */ }
            writer = null;
        }

        try {
            socket.close();
        }
        catch (Exception e) {
            // Ignore.
        }

        saslAuthentication.init();
    }

    public void disconnect(Presence unavailablePresence) {
        // If not connected, ignore this request.
        if (packetReader == null || packetWriter == null) {
            return;
        }

        shutdown(unavailablePresence);

//        if (roster != null) {
//            roster.cleanup();
//            roster = null;
//        }

//        wasAuthenticated = false;
//
//        packetWriter.cleanup();
//        packetWriter = null;
//        packetReader.cleanup();
//        packetReader = null;
    }

    public void sendPacket(Packet packet) {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to server.");
        }
        if (packet == null) {
            throw new NullPointerException("Packet is null.");
        }
        packetWriter.sendPacket(packet);
    }

    /**
     * Registers a packet interceptor with this connection. The interceptor will be
     * invoked every time a packet is about to be sent by this connection. Interceptors
     * may modify the packet to be sent. A packet filter determines which packets
     * will be delivered to the interceptor.
     *
     * @param packetInterceptor the packet interceptor to notify of packets about to be sent.
     * @param packetFilter      the packet filter to use.
     * @deprecated replaced by {@link Connection#addPacketInterceptor(PacketInterceptor, PacketFilter)}.
     */
    public void addPacketWriterInterceptor(PacketInterceptor packetInterceptor,
            PacketFilter packetFilter) {
        addPacketInterceptor(packetInterceptor, packetFilter);
    }

    /**
     * Removes a packet interceptor.
     *
     * @param packetInterceptor the packet interceptor to remove.
     * @deprecated replaced by {@link Connection#removePacketInterceptor(PacketInterceptor)}.
     */
    public void removePacketWriterInterceptor(PacketInterceptor packetInterceptor) {
        removePacketInterceptor(packetInterceptor);
    }

    /**
     * Registers a packet listener with this connection. The listener will be
     * notified of every packet that this connection sends. A packet filter determines
     * which packets will be delivered to the listener. Note that the thread
     * that writes packets will be used to invoke the listeners. Therefore, each
     * packet listener should complete all operations quickly or use a different
     * thread for processing.
     *
     * @param packetListener the packet listener to notify of sent packets.
     * @param packetFilter   the packet filter to use.
     * @deprecated replaced by {@link #addPacketSendingListener(PacketListener, PacketFilter)}.
     */
    public void addPacketWriterListener(PacketListener packetListener, PacketFilter packetFilter) {
        addPacketSendingListener(packetListener, packetFilter);
    }

    /**
     * Removes a packet listener for sending packets from this connection.
     *
     * @param packetListener the packet listener to remove.
     * @deprecated replaced by {@link #removePacketSendingListener(PacketListener)}.
     */
    public void removePacketWriterListener(PacketListener packetListener) {
        removePacketSendingListener(packetListener);
    }

    private void connectUsingConfiguration(ConnectionConfiguration config) throws XMPPException {
        String host = config.getHost();
        int port = config.getPort();
        try {
            if (config.getSocketFactory() == null) {
                this.socket = new Socket(host, port);
            }
            else {
                this.socket = config.getSocketFactory().createSocket(host, port);
            }
        }
        catch (UnknownHostException uhe) {
            String errorMessage = "Could not connect to " + host + ":" + port + ".";
            throw new XMPPException(errorMessage, new XMPPError(
                    XMPPError.Condition.remote_server_timeout, errorMessage),
                    uhe);
        }
        catch (IOException ioe) {
            String errorMessage = "XMPPError connecting to " + host + ":"
                    + port + ".";
            throw new XMPPException(errorMessage, new XMPPError(
                    XMPPError.Condition.remote_server_error, errorMessage), ioe);
        }
        initConnection();
    }

    /**
     * Initializes the connection by creating a packet reader and writer and opening a
     * XMPP stream to the server.
     *
     * @throws XMPPException if establishing a connection to the server fails.
     */
    private void initConnection() throws XMPPException {
        boolean isFirstInitialization = packetReader == null || packetWriter == null;
        if (!isFirstInitialization) {
            usingCompression = false;
        }

        // Set the reader and writer instance variables
        initReaderAndWriter();

        try {
            if (isFirstInitialization) {
                packetWriter = new PacketWriter(this);
                packetReader = new PacketReader(this);

                // If debugging is enabled, we should start the thread that will listen for
                // all packets and then log them.
                if (config.isDebuggerEnabled()) {
                    addPacketListener(debugger.getReaderListener(), null);
                    if (debugger.getWriterListener() != null) {
                        addPacketSendingListener(debugger.getWriterListener(), null);
                    }
                }
            }
            else {
                packetWriter.init();
                packetReader.init();
            }

            // Start the packet writer. This will open a XMPP stream to the server
            packetWriter.startup();
            // Start the packet reader. The startup() method will block until we
            // get an opening stream packet back from server.
            packetReader.startup();

            // Make note of the fact that we're now connected.
            connected = true;

            // Start keep alive process (after TLS was negotiated - if available)
            packetWriter.startKeepAliveProcess();


            if (isFirstInitialization) {
                // Notify listeners that a new connection has been established
                for (ConnectionCreationListener listener : getConnectionCreationListeners()) {
                    listener.connectionCreated(this);
                }
            }
            else if (!wasAuthenticated) {
                packetReader.notifyReconnection();
            }

        }
        catch (XMPPException ex) {
            // An exception occurred in setting up the connection. Make sure we shut down the
            // readers and writers and close the socket.

            if (packetWriter != null) {
                try {
                    packetWriter.shutdown();
                }
                catch (Throwable ignore) { /* ignore */ }
                packetWriter = null;
            }
            if (packetReader != null) {
                try {
                    packetReader.shutdown();
                }
                catch (Throwable ignore) { /* ignore */ }
                packetReader = null;
            }
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (Throwable ignore) { /* ignore */ }
                reader = null;
            }
            if (writer != null) {
                try {
                    writer.close();
                }
                catch (Throwable ignore) {  /* ignore */}
                writer = null;
            }
            if (socket != null) {
                try {
                    socket.close();
                }
                catch (Exception e) { /* ignore */ }
                socket = null;
            }
            this.setWasAuthenticated(authenticated);
            authenticated = false;
            connected = false;

            throw ex;        // Everything stoppped. Now throw the exception.
        }
    }

    private void initReaderAndWriter() throws XMPPException {
        try {
            if (!usingCompression) {
                reader =
                        new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            }
            else {
                try {
                    Class<?> zoClass = Class.forName("com.jcraft.jzlib.ZOutputStream");
                    Constructor<?> constructor = zoClass.getConstructor(OutputStream.class, Integer.TYPE);
                    Object out = constructor.newInstance(socket.getOutputStream(), 9);
                    Method method = zoClass.getMethod("setFlushMode", Integer.TYPE);
                    method.invoke(out, 2);
                    writer = new BufferedWriter(new OutputStreamWriter((OutputStream) out, "UTF-8"));

                    Class<?> ziClass = Class.forName("com.jcraft.jzlib.ZInputStream");
                    constructor = ziClass.getConstructor(InputStream.class);
                    Object in = constructor.newInstance(socket.getInputStream());
                    method = ziClass.getMethod("setFlushMode", Integer.TYPE);
                    method.invoke(in, 2);
                    reader = new BufferedReader(new InputStreamReader((InputStream) in, "UTF-8"));
                }
                catch (Exception e) {
                    e.printStackTrace();
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
                }
            }
        }
        catch (IOException ioe) {
            throw new XMPPException(
                    "XMPPError establishing connection with server.",
                    new XMPPError(XMPPError.Condition.remote_server_error,
                            "XMPPError establishing connection with server."),
                    ioe);
        }

        // If debugging is enabled, we open a window and write out all network traffic.
        initDebugger();
    }

    /***********************************************
     * TLS code below
     **********************************************/

    /**
     * Returns true if the connection to the server has successfully negotiated TLS. Once TLS
     * has been negotiatied the connection has been secured.
     *
     * @return true if the connection to the server has successfully negotiated TLS.
     */
    public boolean isUsingTLS() {
        return usingTLS;
    }

    /**
     * Notification message saying that the server supports TLS so confirm the server that we
     * want to secure the connection.
     *
     * @param required true when the server indicates that TLS is required.
     */
    void startTLSReceived(boolean required) {
        if (required && config.getSecurityMode() == ConnectionConfiguration.SecurityMode.disabled) {
            packetReader.notifyConnectionError(new IllegalStateException("TLS required by server"));
            return;
        }

        if (config.getSecurityMode() == ConnectionConfiguration.SecurityMode.disabled) {
            return;
        }
        try {
            writer.write("<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>");
            writer.flush();
        }
        catch (IOException e) {
            packetReader.notifyConnectionError(e);
        }
    }

    /**
     * The server has indicated that TLS negotiation can start. We now need to secure the
     * existing plain connection and perform a handshake. This method won't return until the
     * connection has finished the handshake or an error occured while securing the connection.
     *
     * @throws Exception if an exception occurs.
     */
    void proceedTLSReceived() throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        KeyStore ks = null;
        KeyManager[] kms = null;
        PasswordCallback pcb = null;

        if(config.getCallbackHandler() == null) {
           ks = null;
        } else {
            //System.out.println("Keystore type: "+configuration.getKeystoreType());
            if(config.getKeystoreType().equals("NONE")) {
                ks = null;
                pcb = null;
            }
            else if(config.getKeystoreType().equals("PKCS11")) {
                try {
                    Constructor<?> c = Class.forName("sun.security.pkcs11.SunPKCS11").getConstructor(InputStream.class);
                    String pkcs11Config = "name = SmartCard\nlibrary = "+config.getPKCS11Library();
                    ByteArrayInputStream config = new ByteArrayInputStream(pkcs11Config.getBytes());
                    Provider p = (Provider)c.newInstance(config);
                    Security.addProvider(p);
                    ks = KeyStore.getInstance("PKCS11",p);
                    pcb = new PasswordCallback("PKCS11 Password: ",false);
                    this.config.getCallbackHandler().handle(new Callback[]{pcb});
                    ks.load(null,pcb.getPassword());
                }
                catch (Exception e) {
                    ks = null;
                    pcb = null;
                }
            }
            else if(config.getKeystoreType().equals("Apple")) {
                ks = KeyStore.getInstance("KeychainStore","Apple");
                ks.load(null,null);
                //pcb = new PasswordCallback("Apple Keychain",false);
                //pcb.setPassword(null);
            }
            else {
                ks = KeyStore.getInstance(config.getKeystoreType());
                try {
                    pcb = new PasswordCallback("Keystore Password: ",false);
                    config.getCallbackHandler().handle(new Callback[]{pcb});
                    ks.load(new FileInputStream(config.getKeystorePath()), pcb.getPassword());
                }
                catch(Exception e) {
                    ks = null;
                    pcb = null;
                }
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            try {
                if(pcb == null) {
                    kmf.init(ks,null);
                } else {
                    kmf.init(ks,pcb.getPassword());
                    pcb.clearPassword();
                }
                kms = kmf.getKeyManagers();
            } catch (NullPointerException npe) {
                kms = null;
            }
        }

        // Verify certificate presented by the server
        context.init(kms,
                new javax.net.ssl.TrustManager[]{new ServerTrustManager(getServiceName(), config)},
                new java.security.SecureRandom());
        Socket plain = socket;
        // Secure the plain connection
        socket = context.getSocketFactory().createSocket(plain,
                plain.getInetAddress().getHostName(), plain.getPort(), true);
        socket.setSoTimeout(0);
        socket.setKeepAlive(true);
        // Initialize the reader and writer with the new secured version
        initReaderAndWriter();
        // Proceed to do the handshake
        ((SSLSocket) socket).startHandshake();
        //if (((SSLSocket) socket).getWantClientAuth()) {
        //    System.err.println("Connection wants client auth");
        //}
        //else if (((SSLSocket) socket).getNeedClientAuth()) {
        //    System.err.println("Connection needs client auth");
        //}
        //else {
        //    System.err.println("Connection does not require client auth");
       // }
        // Set that TLS was successful
        usingTLS = true;

        // Set the new  writer to use
        packetWriter.setWriter(writer);
        // Send a new opening stream to the server
        packetWriter.openStream();
    }

    /**
     * Sets the available stream compression methods offered by the server.
     *
     * @param methods compression methods offered by the server.
     */
    void setAvailableCompressionMethods(Collection<String> methods) {
        compressionMethods = methods;
    }

    /**
     * Returns true if the specified compression method was offered by the server.
     *
     * @param method the method to check.
     * @return true if the specified compression method was offered by the server.
     */
    private boolean hasAvailableCompressionMethod(String method) {
        return compressionMethods != null && compressionMethods.contains(method);
    }

    public boolean isUsingCompression() {
        return usingCompression;
    }

    /**
     * Starts using stream compression that will compress network traffic. Traffic can be
     * reduced up to 90%. Therefore, stream compression is ideal when using a slow speed network
     * connection. However, the server and the client will need to use more CPU time in order to
     * un/compress network data so under high load the server performance might be affected.<p>
     * <p/>
     * Stream compression has to have been previously offered by the server. Currently only the
     * zlib method is supported by the client. Stream compression negotiation has to be done
     * before authentication took place.<p>
     * <p/>
     * Note: to use stream compression the smackx.jar file has to be present in the classpath.
     *
     * @return true if stream compression negotiation was successful.
     */
    private boolean useCompression() {
        // If stream compression was offered by the server and we want to use
        // compression then send compression request to the server
        if (authenticated) {
            throw new IllegalStateException("Compression should be negotiated before authentication.");
        }
        try {
            Class.forName("com.jcraft.jzlib.ZOutputStream");
        }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot use compression.");
        }
        if (hasAvailableCompressionMethod("zlib")) {
            requestStreamCompression();
            // Wait until compression is being used or a timeout happened
            synchronized (this) {
                try {
                    this.wait(10000);
                }
                catch (InterruptedException e) { }
            }
            return usingCompression;
        }
        return false;
    }

    /**
     * Request the server that we want to start using stream compression. When using TLS
     * then negotiation of stream compression can only happen after TLS was negotiated. If TLS
     * compression is being used the stream compression should not be used.
     */
    private void requestStreamCompression() {
        try {
            writer.write("<compress xmlns='http://jabber.org/protocol/compress'>");
            writer.write("<method>zlib</method></compress>");
            writer.flush();
        }
        catch (IOException e) {
            packetReader.notifyConnectionError(e);
        }
    }

    /**
     * Start using stream compression since the server has acknowledged stream compression.
     *
     * @throws Exception if there is an exception starting stream compression.
     */
    void startStreamCompression() throws Exception {
        usingCompression = true;
        initReaderAndWriter();

        packetWriter.setWriter(writer);
        packetWriter.openStream();
        synchronized (this) {
            this.notify();
        }
    }

    /**
     * Notifies the XMPP connection that stream compression was denied so that
     * the connection process can proceed.
     */
    void streamCompressionDenied() {
        synchronized (this) {
            this.notify();
        }
    }

    /**
     * Establishes a connection to the XMPP server and performs an automatic login
     * only if the previous connection state was logged (authenticated). It basically
     * creates and maintains a socket connection to the server.<p>
     * <p/>
     * Listeners will be preserved from a previous connection if the reconnection
     * occurs after an abrupt termination.
     *
     * @throws XMPPException if an error occurs while trying to establish the connection.
     *      Two possible errors can occur which will be wrapped by an XMPPException --
     *      UnknownHostException (XMPP error code 504), and IOException (XMPP error code
     *      502). The error codes and wrapped exceptions can be used to present more
     *      appropiate error messages to end-users.
     */
    public void connect() throws XMPPException {
        // Stablishes the connection, readers and writers
        connectUsingConfiguration(config);
        // Automatically makes the login if the user was previouslly connected successfully
        // to the server and the connection was terminated abruptly
//        if (connected && wasAuthenticated) {
//            // Make the login
//            try {
//                if (isAnonymous()) {
//                    // Make the anonymous login
//                    loginAnonymously();
//                }
//                else {
//                    login(config.getUsername(), config.getPassword(),
//                            config.getResource());
//                }
//                packetReader.notifyReconnection();
//            }
//            catch (XMPPException e) {
//                e.printStackTrace();
//            }
//        }
    }

    /**
     * Sets whether the connection has already logged in the server.
     *
     * @param wasAuthenticated true if the connection has already been authenticated.
     */
    private void setWasAuthenticated(boolean wasAuthenticated) {
        if (!this.wasAuthenticated) {
            this.wasAuthenticated = wasAuthenticated;
        }
    }

	@Override
	public void setRosterStorage(RosterStorage storage)
			throws IllegalStateException {
		if(roster != null){
			throw new IllegalStateException("Roster is already initialized");
		}
		this.rosterStorage = storage;
	}
}
