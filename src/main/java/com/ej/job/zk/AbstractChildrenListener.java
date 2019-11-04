package com.ej.job.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

public abstract class AbstractChildrenListener extends BaseService {

    public AbstractChildrenListener() {
        super();
    }

    public AbstractChildrenListener(CuratorFramework client) {
        super(client);
    }

    protected abstract String zkPath();

    public void listen() throws Exception {
        PathChildrenCache cache = new PathChildrenCache(this.client, this.zkPath(), false);
        cache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                switch (event.getType()) {
                    case CHILD_ADDED:
                        add(client, event.getData().getPath());
                        break;
                    case CHILD_UPDATED:
                        update(client, event.getData().getPath());
                        break;
                    case CHILD_REMOVED:
                        remove(client, event.getData().getPath());
                        break;
                    default:
                        break;
                }
            }
        });
        cache.start();
    }

    protected abstract void add(CuratorFramework client, String path);

    protected abstract void remove(CuratorFramework client, String path);

    protected abstract void update(CuratorFramework client, String path);
}