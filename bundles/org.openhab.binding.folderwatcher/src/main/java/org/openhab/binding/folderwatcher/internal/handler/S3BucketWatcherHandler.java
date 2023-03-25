/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.folderwatcher.internal.handler;

import static org.openhab.binding.folderwatcher.internal.FolderWatcherBindingConstants.CHANNEL_NEWS3FILE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.folderwatcher.internal.common.WatcherCommon;
import org.openhab.binding.folderwatcher.internal.config.S3BucketWatcherConfiguration;
import org.openhab.core.OpenHAB;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * The {@link S3BucketWatcherHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexandr Salamatov - Initial contribution
 */
@NonNullByDefault
public class S3BucketWatcherHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(S3BucketWatcherHandler.class);
    private S3BucketWatcherConfiguration config = new S3BucketWatcherConfiguration();
    private File currentS3ListingFile = new File(OpenHAB.getUserDataFolder() + File.separator + "FolderWatcher"
            + File.separator + thing.getUID().getAsString().replace(':', '_') + ".data");
    private @Nullable ScheduledFuture<?> executionJob;
    private List<String> previousS3Listing = new ArrayList<>();
    private @Nullable S3Client client;
    private @Nullable AwsCredentialsProvider credentialsProvider;
    private @Nullable Region region;

    public S3BucketWatcherHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Channel {} triggered with command {}", channelUID.getId(), command);
        if (command instanceof RefreshType) {
            refreshS3BucketInformation();
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(S3BucketWatcherConfiguration.class);
        updateStatus(ThingStatus.UNKNOWN);

        try {
            previousS3Listing = WatcherCommon.initStorage(currentS3ListingFile, config.s3BucketName);
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            logger.debug("Can't write file {}: {}", currentS3ListingFile, e.getMessage());
            return;
        }
        region = null;
        for (Region reg : Region.regions()) {
            if (reg.toString().equals(config.awsRegion)) {
                region = Region.of(config.awsRegion);
            }
        }
        if (region == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Please enter valid region");
            return;
        }
        String accessKey = config.awsKey;
        String secretKey = config.awsSecret;
        AwsCredentials credentials;

        if (config.s3Anonymous) {
            credentialsProvider = AnonymousCredentialsProvider.create();
        } else {
            if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
                credentials = AwsBasicCredentials.create(accessKey, secretKey);
                credentialsProvider = StaticCredentialsProvider.create(credentials);
            }
        }

        if (refreshS3BucketInformation()) {
            if (config.pollIntervalS3 > 0) {
                updateStatus(ThingStatus.ONLINE);
                executionJob = scheduler.scheduleWithFixedDelay(this::refreshS3BucketInformation, config.pollIntervalS3,
                        config.pollIntervalS3, TimeUnit.SECONDS);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Polling interval must be greather then 0 seconds");
                return;
            }
        }
    }

    private boolean refreshS3BucketInformation() {
        List<String> currentS3Listing = new ArrayList<>();
        try {
            client = S3Client.builder().region(region).credentialsProvider(credentialsProvider).build();
            ListObjectsRequest listObjects = ListObjectsRequest.builder().bucket(config.s3BucketName).build();
            ListObjectsResponse res = client.listObjects(listObjects);
            List<S3Object> objects = res.contents();

            for (S3Object s3Obj : objects) {
                if (s3Obj.key().startsWith(config.s3Path)) {
                    currentS3Listing.add(s3Obj.key());
                }
            }
            client.close();

            List<String> difS3Listing = new ArrayList<>(currentS3Listing);
            difS3Listing.removeAll(previousS3Listing);
            difS3Listing.forEach(file -> triggerChannel(CHANNEL_NEWS3FILE, file));

            if (!difS3Listing.isEmpty()) {
                WatcherCommon.saveNewListing(difS3Listing, currentS3ListingFile);
            }
            previousS3Listing = new ArrayList<>(currentS3Listing);
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Can't connect to the bucket");
            logger.debug("Can't connect to the bucket: {}", e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> executionJob = this.executionJob;
        if (executionJob != null) {
            executionJob.cancel(true);
        }
    }
}
