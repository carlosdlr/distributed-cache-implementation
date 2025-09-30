package com.jikkosoft.cache.service;

import com.jikkosoft.cache.model.*;

public interface NotificationService {
    /**
     * Notifies peer nodes about a cache update.
     *
     * @param path  The path to notify on the peer nodes.
     * @param update The cache update to notify.
     */
    void notifyPeers(String path, CacheUpdate update);

    /**
     * Shuts down the notification service, releasing any resources.
     */
    void shutdown();
}
