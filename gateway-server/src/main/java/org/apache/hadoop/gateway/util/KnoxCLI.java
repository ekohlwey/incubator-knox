/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.gateway.util;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.gateway.GatewayCommandLine;
import org.apache.hadoop.gateway.config.GatewayConfig;
import org.apache.hadoop.gateway.config.impl.GatewayConfigImpl;
import org.apache.hadoop.gateway.services.CLIGatewayServices;
import org.apache.hadoop.gateway.services.GatewayServices;
import org.apache.hadoop.gateway.services.Service;
import org.apache.hadoop.gateway.services.ServiceLifecycleException;
import org.apache.hadoop.gateway.services.security.AliasService;
import org.apache.hadoop.gateway.services.security.KeystoreService;
import org.apache.hadoop.gateway.services.security.KeystoreServiceException;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 */
public class KnoxCLI extends Configured implements Tool {

  private static final String USAGE_PREFIX = "KnoxCLI {cmd} [options]";
  final static private String COMMANDS =
      "   [--help]\n" +
      "   [" + CertCreateCommand.USAGE + "]\n" +
      "   [" + MasterCreateCommand.USAGE + "]\n" +
      "   [" + AliasCreateCommand.USAGE + "]\n" +
      "   [" + AliasDeleteCommand.USAGE + "]\n" +
      "   [" + AliasListCommand.USAGE + "]\n";

  /** allows stdout to be captured if necessary */
  public PrintStream out = System.out;
  /** allows stderr to be captured if necessary */
  public PrintStream err = System.err;
  
  private static GatewayServices services = new CLIGatewayServices();
  private Command command;
  private String value = null;
  private String cluster = null;
  private String generate = "false";
  private String hostname = null;
  
  // for testing only
  private String master = null;

  /* (non-Javadoc)
   * @see org.apache.hadoop.util.Tool#run(java.lang.String[])
   */
  @Override
  public int run(String[] args) throws Exception {
    int exitCode = 0;
    try {
      exitCode = init(args);
      if (exitCode != 0) {
        return exitCode;
      }
      if (command.validate()) {
          initializeServices(command instanceof MasterCreateCommand);
          command.execute();
      } else {
        exitCode = -1;
      }
    } catch (Exception e) {
      e.printStackTrace(err);
      return -1;
    }
    return exitCode;
  }

  private void initializeServices(boolean persisting) throws ServiceLifecycleException {
    GatewayConfig config = new GatewayConfigImpl();
    Map<String,String> options = new HashMap<String,String>();
    options.put(GatewayCommandLine.PERSIST_LONG, Boolean.toString(persisting));
    if (master != null) {
      options.put("master", master);
    }
    services.init(config, options);
  }

  /**
   * Parse the command line arguments and initialize the data
   * <pre>
   * % knox master-create keyName [--size size] [--generate]
   *    [--provider providerPath]
   * % knox create-alias alias [--value v]
   * % knox list-alias [-provider providerPath]
   * % knox delete=alias keyName [--provider providerPath] [-i]
   * % knox create-cert keyName [--provider providerPath] [-i]
   * </pre>
   * @param args
   * @return
   * @throws IOException
   */
  private int init(String[] args) throws IOException {
    for (int i = 0; i < args.length; i++) { // parse command line
      if (args[i].equals("create-master")) {
        command = new MasterCreateCommand();
        if ((args.length > i + 1) && args[i + 1].equals("--help")) {
          printKnoxShellUsage();
          return -1;
        }
      } else if (args[i].equals("delete-alias")) {
        String alias = args[++i];
        command = new AliasDeleteCommand(alias);
        if (alias.equals("--help")) {
          printKnoxShellUsage();
          return -1;
        }
      } else if (args[i].equals("create-alias")) {
        String alias = args[++i];
        command = new AliasCreateCommand(alias);
        if (alias.equals("--help")) {
          printKnoxShellUsage();
          return -1;
        }
      } else if (args[i].equals("create-cert")) {
        command = new CertCreateCommand();
        if ((args.length > i + 1) && args[i + 1].equals("--help")) {
          printKnoxShellUsage();
          return -1;
        }
      } else if (args[i].equals("list-alias")) {
        command = new AliasListCommand();
      } else if (args[i].equals("--value")) {
        this.value = args[++i];
      } else if (args[i].equals("--cluster")) {
        this.cluster = args[++i];
      } else if (args[i].equals("--generate")) {
        this.generate = "true";
      } else if (args[i].equals("--hostname")) {
        this.hostname = args[++i];
      } else if (args[i].equals("--master")) {
        // testing only
        this.master = args[++i];
      } else if (args[i].equals("--help")) {
        printKnoxShellUsage();
        return -1;
      } else {
        printKnoxShellUsage();
        ToolRunner.printGenericCommandUsage(System.err);
        return -1;
      }
    }
    return 0;
  }

  private void printKnoxShellUsage() {
    out.println(USAGE_PREFIX + COMMANDS);
    if (command != null) {
      out.println(command.getUsage());
    }
    else {
      out.println("=========================================================" +
          "======");
      out.println(MasterCreateCommand.USAGE + ":\n\n" + MasterCreateCommand.DESC);
      out.println("=========================================================" +
          "======");
      out.println(CertCreateCommand.USAGE + ":\n\n" + CertCreateCommand.DESC);
      out.println("=========================================================" +
          "======");
      out.println(AliasCreateCommand.USAGE + ":\n\n" + AliasCreateCommand.DESC);
      out.println("=========================================================" +
          "======");
      out.println(AliasDeleteCommand.USAGE + ":\n\n" + AliasDeleteCommand.DESC);
      out.println("=========================================================" +
          "======");
      out.println(AliasListCommand.USAGE + ":\n\n" + AliasListCommand.DESC);
    }
  }

  private abstract class Command {
    protected Service provider = null;

    public boolean validate() {
      return true;
    }

    protected Service getService(String serviceName) {
      Service service = null;

      return service;
    }

    public abstract void execute() throws Exception;

    public abstract String getUsage();

    protected AliasService getAliasService() {
      AliasService as = (AliasService) 
           services.getService(GatewayServices.ALIAS_SERVICE);
      return as;
    }

    protected KeystoreService getKeystoreService() {
      KeystoreService ks = (KeystoreService) 
           services.getService(GatewayServices.KEYSTORE_SERVICE);
      return ks;
    }
  }
  
  /**
  *
  */
 private class AliasListCommand extends Command {

  public static final String USAGE = "list-alias [--cluster c]";
  public static final String DESC = "The list-alias command lists all of the aliases\n" +
  		                               "for the given hadoop --cluster. The default\n" +
  		                               "--cluster being the gateway itself.";

  /* (non-Javadoc)
    * @see org.apache.hadoop.gateway.util.KnoxCLI.Command#execute()
    */
   @Override
   public void execute() throws Exception {
     AliasService as = getAliasService();

     if (cluster == null) {
       cluster = "__gateway";
     }
     out.println("Listing aliases for: " + cluster);
     List<String> aliases = as.getAliasesForCluster(cluster);
     for (String alias : aliases) {
       out.println(alias);
     }
     out.println("\n" + aliases.size() + " items.");
   }

   /* (non-Javadoc)
    * @see org.apache.hadoop.gateway.util.KnoxCLI.Command#getUsage()
    */
   @Override
   public String getUsage() {
     return USAGE + ":\n\n" + DESC;
   }
 }

 /**
  *
  */
 public class CertCreateCommand extends Command {

  public static final String USAGE = "create-cert [--hostname h]";
  public static final String DESC = "The create-cert command creates and populates\n" +
  		                               "a gateway.jks keystore with a self-signed certificate\n" +
  		                               "to be used as the gateway identity. It also adds an alias\n" +
  		                               "to the __gateway-credentials.jceks credential store for the\n" +
  		                               "key passphrase.";
  private static final String GATEWAY_CREDENTIAL_STORE_NAME = "__gateway";
  private static final String GATEWAY_IDENTITY_PASSPHRASE = "gateway-identity-passphrase";

  /**
    * 
    */
   public CertCreateCommand() {
     // TODO Auto-generated constructor stub
   }

   /* (non-Javadoc)
    * @see org.apache.hadoop.gateway.util.KnoxCLI.Command#execute()
    */
   @Override
   public void execute() throws Exception {
     KeystoreService ks = getKeystoreService();
     
     AliasService as = getAliasService();
     
     if (ks != null) {
       try {
         if (!ks.isCredentialStoreForClusterAvailable(GATEWAY_CREDENTIAL_STORE_NAME)) {
//           log.creatingCredentialStoreForGateway();
           ks.createCredentialStoreForCluster(GATEWAY_CREDENTIAL_STORE_NAME);
         }
         else {
//           log.credentialStoreForGatewayFoundNotCreating();
         }
         as.generateAliasForCluster(GATEWAY_CREDENTIAL_STORE_NAME, GATEWAY_IDENTITY_PASSPHRASE);
       } catch (KeystoreServiceException e) {
         throw new ServiceLifecycleException("Keystore was not loaded properly - the provided (or persisted) master secret may not match the password for the keystore.", e);
       }
  
       try {
         if (!ks.isKeystoreForGatewayAvailable()) {
//           log.creatingKeyStoreForGateway();
           ks.createKeystoreForGateway();
         }
         else {
//           log.keyStoreForGatewayFoundNotCreating();
         }
         char[] passphrase = as.getPasswordFromAliasForCluster(GATEWAY_CREDENTIAL_STORE_NAME, GATEWAY_IDENTITY_PASSPHRASE);
         ks.addSelfSignedCertForGateway("gateway-identity", passphrase, hostname);
//         logAndValidateCertificate();
         out.println("gateway-identity has been successfully created.");
       } catch (KeystoreServiceException e) {
         throw new ServiceLifecycleException("Keystore was not loaded properly - the provided (or persisted) master secret may not match the password for the keystore.", e);
       }
     }
   }

  /* (non-Javadoc)
    * @see org.apache.hadoop.gateway.util.KnoxCLI.Command#getUsage()
    */
   @Override
   public String getUsage() {
     return USAGE + ":\n\n" + DESC;
   }

 }

 /**
  *
  */
 public class AliasCreateCommand extends Command {

  public static final String USAGE = "create-alias aliasname [--value value]" +
  		                              " [--cluster c] [--generate]";
  public static final String DESC = "The create-alias command will create an alias\n" +
  		                               "and secret pair within the credential store for the\n" +
  		                               "indicated --cluster otherwise within the gateway\n" +
  		                               "credential store. The actual secret may be specified via\n" +
  		                               "the --value option or --generate will create a random secret\n" +
  		                               "for you.";
  
  private String name = null; 

  /**
    * @param alias
    */
   public AliasCreateCommand(String alias) {
     name = alias;
   }

   /* (non-Javadoc)
    * @see org.apache.hadoop.gateway.util.KnoxCLI.Command#execute()
    */
   @Override
   public void execute() throws Exception {
     AliasService as = getAliasService();
     if (cluster == null) {
       cluster = "__gateway";
     }
     if (value != null) {
       as.addAliasForCluster(cluster, name, value);
       out.println(name + " has been successfully created.");
     }
     else {
       if (generate.equals("true")) {
         as.generateAliasForCluster(cluster, name);
         out.println(name + " has been successfully generated.");
       }
       else {
         throw new IllegalArgumentException("No value has been set. " +
         		"Consider setting --generate or --value.");
       }
     }
   }

   /* (non-Javadoc)
    * @see org.apache.hadoop.gateway.util.KnoxCLI.Command#getUsage()
    */
   @Override
   public String getUsage() {
     return USAGE + ":\n\n" + DESC;
   }

 }

 /**
  *
  */
 public class AliasDeleteCommand extends Command {
  public static final String USAGE = "delete-alias aliasname [--cluster c]";
  public static final String DESC = "The delete-alias command removes the\n" +
  		                               "indicated alias from the --cluster specific\n" +
  		                               "credential store or the gateway credential store.";
  
  private String name = null;

  /**
    * @param alias
    */
   public AliasDeleteCommand(String alias) {
     name = alias;
   }

   /* (non-Javadoc)
    * @see org.apache.hadoop.gateway.util.KnoxCLI.Command#execute()
    */
   @Override
   public void execute() throws Exception {
     AliasService as = getAliasService();
     if (as != null) {
       if (cluster == null) {
         cluster = "__gateway";
       }
       as.removeAliasForCluster(cluster, name);
       out.println(name + " has been successfully deleted.");
     }
   }

   /* (non-Javadoc)
    * @see org.apache.hadoop.gateway.util.KnoxCLI.Command#getUsage()
    */
   @Override
   public String getUsage() {
     return USAGE + ":\n\n" + DESC;
   }

 }

 /**
  *
  */
 public class MasterCreateCommand extends Command {
  public static final String USAGE = "create-master";
  public static final String DESC = "The create-master command persists the\n" +
  		                               "master secret in a file located at:\n" +
  		                               "{GATEWAY_HOME}/data/security/master. It\n" +
  		                               "will prompt the user for the secret to persist.";

  /**
    * @param keyName
    */
   public MasterCreateCommand() {
   }

   /* (non-Javadoc)
    * @see org.apache.hadoop.gateway.util.KnoxCLI.Command#execute()
    */
   @Override
   public void execute() throws Exception {
     out.println("Master secret has been persisted to disk.");
   }

   /* (non-Javadoc)
    * @see org.apache.hadoop.gateway.util.KnoxCLI.Command#getUsage()
    */
   @Override
   public String getUsage() {
     return USAGE + ":\n\n" + DESC;
   }
 }

 /**
  * @param args
  * @throws Exception 
  */
  public static void main(String[] args) throws Exception {
    PropertyConfigurator.configure( System.getProperty( "log4j.configuration" ) );
    int res = ToolRunner.run(new GatewayConfigImpl(), new KnoxCLI(), args);
    System.exit(res);
  }
}
