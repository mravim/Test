package jdbc;

import org.hsqldb.DatabaseManager;
import org.hsqldb.Server;
import org.hsqldb.ServerConstants;
import org.hsqldb.jdbc.jdbcDataSource;

import javax.naming.*;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;

public class ServerWrapper {

    private static final String DATA_SOURCE_NAME = "jdbc/testDB";

    private static final String DB_NAME = "test";
    private static final String TEST_PROPERTIES = DB_NAME + ".properties";
    private static final String TEST_SCRIPT = DB_NAME+ ".script";

    private Server server;
    private Context context;
    private File tempDir;

    public void start() throws NamingException, IOException {
        server = new Server();
        tempDir = File.createTempFile("hsql", "");
        tempDir.delete();
        tempDir.mkdir();

        copyFile("db/" + TEST_PROPERTIES, new File(tempDir, TEST_PROPERTIES));
        copyFile("db/" + TEST_SCRIPT, new File(tempDir, TEST_SCRIPT));

        server.setDatabasePath(0, "file:" + tempDir.getPath().replace('\\', '/') + "/" + DB_NAME);
        server.setSilent(true);
        server.start();

        while (server.getState() != ServerConstants.SERVER_STATE_ONLINE) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        context = createContext();
        System.out.println("Server started");
    }

    public void stop() {
        System.out.println("Stopping server ...");
        DatabaseManager.closeDatabases(0);
        server.stop();
        while (server.getState() != ServerConstants.SERVER_STATE_SHUTDOWN) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (File file : tempDir.listFiles()) {
            file.delete();
        }
        tempDir.delete();
        System.out.println("Server stopped");
    }

    public Connection getDataSourceConnection() throws SQLException, NamingException {
        return ((DataSource)context.lookup(DATA_SOURCE_NAME)).getConnection();
    }

    private static Context createContext() throws NamingException {
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,  MyInitialContextFactory.class.getName());
        return new InitialContext(env);
    }

    public static class MyInitialContextFactory implements InitialContextFactory {
        public Context getInitialContext(Hashtable environment) throws NamingException {
            return new Context() {

                private jdbcDataSource dataSource;

                public Object lookup(Name name) throws NamingException {
                    return null;
                }
                public Object lookup(String name) throws NamingException {
                    if (name.equals(DATA_SOURCE_NAME)) {
                        if (dataSource == null) {
                            dataSource = new jdbcDataSource();

                            dataSource.setDatabase("jdbc:hsqldb:hsql://localhost");
                            dataSource.setUser("sa");
                            dataSource.setPassword("");
                        }

                        return dataSource;
                    } else {
                        return null;
                    }
                }
                public void bind(Name name, Object obj) throws NamingException {
                }
                public void bind(String name, Object obj) throws NamingException {
                }
                public void rebind(Name name, Object obj) throws NamingException {
                }
                public void rebind(String name, Object obj) throws NamingException {
                }
                public void unbind(Name name) throws NamingException {
                }
                public void unbind(String name) throws NamingException {
                }
                public void rename(Name oldName, Name newName) throws NamingException {
                }
                public void rename(String oldName, String newName) throws NamingException {
                }
                public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
                    return null;
                }
                public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
                    return null;
                }
                public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
                    return null;
                }
                public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
                    return null;
                }
                public void destroySubcontext(Name name) throws NamingException {
                }
                public void destroySubcontext(String name) throws NamingException {
                }
                public Context createSubcontext(Name name) throws NamingException {
                    return null;
                }
                public Context createSubcontext(String name) throws NamingException {
                    return null;
                }
                public Object lookupLink(Name name) throws NamingException {
                    return null;
                }
                public Object lookupLink(String name) throws NamingException {
                    return null;
                }
                public NameParser getNameParser(Name name) throws NamingException {
                    return null;
                }
                public NameParser getNameParser(String name) throws NamingException {
                    return null;
                }
                public Name composeName(Name name, Name prefix) throws NamingException {
                    return null;
                }
                public String composeName(String name, String prefix) throws NamingException {
                    return null;
                }
                public Object addToEnvironment(String propName, Object propVal) throws NamingException {
                    return null;
                }
                public Object removeFromEnvironment(String propName) throws NamingException {
                    return null;
                }
                public Hashtable getEnvironment() throws NamingException {
                    return null;
                }
                public void close() throws NamingException {
                }
                public String getNameInNamespace() throws NamingException {
                    return null;
                }
            };
        }
    }


    public void copyFile(String relativePath, File targetFile) throws IOException {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = getClass().getResourceAsStream(relativePath);
            out = new FileOutputStream(targetFile);
            byte[] buf = new byte[1024];

            int currentRead;
            while ((currentRead = in.read(buf)) != -1) {
                out.write(buf, 0, currentRead);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

}
