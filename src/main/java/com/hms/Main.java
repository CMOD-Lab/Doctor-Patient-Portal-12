package com.hms;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;

/**
 * Main entry point for the Doctor-Patient-Portal application.
 *
 * cr-java-0107: Replaces WAR deployment to an external application server with
 * an embedded Tomcat server launched from a self-contained executable JAR.
 * This enables direct deployment on AWS ECS, EKS, or Fargate without requiring
 * a pre-installed Tomcat/JBoss/WebLogic instance in the container image.
 *
 * The server port is read from the PORT environment variable (default 8080)
 * following 12-factor app principle III (Config) so that the same artifact
 * can be deployed across dev, staging, and production environments by simply
 * changing the environment variable.
 */
public class Main {

    public static void main(String[] args) throws LifecycleException {

        // 12-factor: read port from environment variable; fall back to 8080
        int port = 8080;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("[Main] Invalid PORT env var '" + portEnv + "', using default 8080");
            }
        }

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector(); // trigger connector creation

        // Locate the webapp directory (works both from IDE and from the fat JAR)
        String webappDirLocation = "src/main/webapp/";
        File webappDir = new File(webappDirLocation);
        if (!webappDir.exists()) {
            // When running from the assembled JAR the webapp resources are on the classpath
            webappDirLocation = ".";
        }

        Context ctx = tomcat.addWebapp("", new File(webappDirLocation).getAbsolutePath());

        // Add compiled classes so Tomcat can find servlets when running from IDE
        File additionWebInfClasses = new File("target/classes");
        if (additionWebInfClasses.exists()) {
            StandardRoot resources = new StandardRoot(ctx);
            resources.addPreResources(
                new DirResourceSet(resources, "/WEB-INF/classes",
                    additionWebInfClasses.getAbsolutePath(), "/"));
            ctx.setResources(resources);
        }

        tomcat.start();
        System.out.println("[Main] Doctor-Patient-Portal started on port " + port);
        tomcat.getServer().await();
    }
}
