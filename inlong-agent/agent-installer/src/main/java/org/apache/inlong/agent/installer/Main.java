/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.installer;

import org.apache.inlong.agent.installer.conf.InstallerConfiguration;
import org.apache.inlong.agent.metrics.audit.AuditUtils;
import org.apache.inlong.common.metric.MetricObserver;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Agent installer entrance class
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    /**
     * Print help information
     *
     * @param opts options
     */
    private static void help(Options opts) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("inlong-agent", opts);
        System.exit(0);
    }

    /**
     * Init options
     *
     * @param args argument
     * @return command line
     */
    public static CommandLine initOptions(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("h", "help", false, "show help");
        try {
            return parser.parse(options, args);
        } catch (Exception ex) {
            help(options);
        }
        return null;
    }

    /**
     * Init agent conf
     *
     * @param cl commandline
     */
    public static void initAgentConf(CommandLine cl) {
        InstallerConfiguration conf = InstallerConfiguration.getInstallerConf();
        Iterator<Option> iterator = cl.iterator();
        while (iterator != null && iterator.hasNext()) {
            Option opt = iterator.next();
            if (opt != null && opt.getLongOpt() != null
                    && opt.getValue() != null && conf.hasKey(opt.getLongOpt())) {
                conf.set(opt.getLongOpt(), opt.getValue().trim());
            }
        }
    }

    /**
     * Stopping installer manager gracefully if it was killed.
     *
     * @param manager installer manager
     */
    private static void stopInstallerIfKilled(Manager manager) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                LOGGER.info("stopping installer gracefully");
                manager.stop();
                AuditUtils.send();
            } catch (Exception ex) {
                LOGGER.error("stop installer manager error: ", ex);
            }
        }));
    }

    /**
     * Main entrance.
     */
    public static void main(String[] args) throws Exception {
        Thread.currentThread().setName("main-thread");
        CommandLine cl = initOptions(args);
        assert cl != null;
        initAgentConf(cl);
        AuditUtils.initAudit();
        Manager manager = new Manager();
        try {
            manager.start();
            stopInstallerIfKilled(manager);
            // metrics
            MetricObserver.init(InstallerConfiguration.getInstallerConf().getConfigProperties());
            manager.join();
        } catch (Exception ex) {
            LOGGER.error("installer running exception: ", ex);
        } finally {
            manager.stop();
        }
    }
}
