/*
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

package org.apache.zookeeper.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.test.ClientBase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class StandaloneServerAuditTest extends ClientBase {
    private static ByteArrayOutputStream os;
    private static final String appenderName = "auditAppender";
    private static LoggerConfig loggerConfig;

    @BeforeClass public static void setup() {
        System.setProperty(ZKAuditProvider.AUDIT_ENABLE, "true");
        // setup the logger to capture all the logs
        os = new ByteArrayOutputStream();
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        Layout layout = PatternLayout.newBuilder().withPattern("%-5p - %m%n").build();
        WriterAppender appender = WriterAppender
                .createAppender((PatternLayout) layout, null, new OutputStreamWriter(os), appenderName, false, true);
        appender.start();
        AppenderRef ref = AppenderRef.createAppenderRef(appenderName, null, null);
        AppenderRef[] refs = new AppenderRef[] { ref };
        loggerConfig = LoggerConfig.createLogger(false, Level.INFO, String.valueOf(Log4jAuditLogger.class), "true", refs, null, config, null);
        loggerConfig.addAppender(appender, null, null);
        config.addLogger("org.apache.zookeeper.audit.Log4jAuditLogger",loggerConfig);
        ctx.updateLoggers(config);
    }

    @AfterClass public static void teardown() {
        System.clearProperty(ZKAuditProvider.AUDIT_ENABLE);
        loggerConfig.removeAppender(appenderName);
    }

    @Test public void testCreateAuditLog() throws KeeperException, InterruptedException, IOException {
        final ZooKeeper zk = createClient();
        String path = "/createPath";
        zk.create(path, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        List<String> logs = readAuditLog(os);
        System.out.println("Audit logs are " + logs);
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).endsWith("operation=create\tznode=/createPath\tznode_type=persistent\tresult=success"));
    }

    private static List<String> readAuditLog(ByteArrayOutputStream os) throws IOException {
        List<String> logs = new ArrayList<>();
        LineNumberReader r = new LineNumberReader(new StringReader(os.toString()));
        String line;
        while ((line = r.readLine()) != null) {
            logs.add(line);
        }
        os.reset();
        return logs;
    }
}

