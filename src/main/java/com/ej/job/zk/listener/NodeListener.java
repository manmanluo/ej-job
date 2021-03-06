package com.ej.job.zk.listener;

import com.ej.job.constants.EJConstants;
import com.ej.job.runner.JobManager;
import com.ej.job.zk.AbstractChildrenListener;
import com.ej.job.zk.option.MasterOptionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

@Slf4j
public class NodeListener extends AbstractChildrenListener {
    MasterOptionService masterOptionService;
    JobManager jobManager;

    public NodeListener() {
        super();
    }

    public NodeListener(CuratorFramework client) {
        super(client);
    }


    public NodeListener(CuratorFramework client, MasterOptionService masterOptionService,JobManager jobManager) {
        super(client);
        this.masterOptionService = masterOptionService;
        this.jobManager = jobManager;
    }

    @Override
    protected String zkPath() {
        return EJConstants.ZK_NODE_BASE;
    }

    @Override
    protected void add(CuratorFramework client, String path) {
        try {
            String nodeName = getNodeName(path);
            cleanPartition();
            log.info("节点[{}]已加入到集群中,需要中断当前任务线程", nodeName);
            if (EJConstants.IS_MASTER.get()) {
                masterOptionService.refresh();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void remove(CuratorFramework client, String path) {
        try {
            String nodeName = getNodeName(path);
            cleanPartition();
            log.info("节点[{}]已从集群中移除,需要中断当前任务线程", nodeName);
            if (EJConstants.IS_MASTER.get()) {
                masterOptionService.refresh();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void update(CuratorFramework client, String path) {
        try {
            String nodeName = getNodeName(path);
            if (!EJConstants.NODE_NAME.equals(nodeName)) {
                return;
            }
            String partitionInterval = new String(client.getData().forPath(path));
            setPartition(partitionInterval);
            log.info("当前节点[{}]已被重新分配任务区间:[{}],需要唤醒当前任务线程", nodeName, partitionInterval);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getNodeName(String path) {
        return path.substring(path.lastIndexOf(EJConstants.SLASH) + 1);
    }

    private void cleanPartition() {
        jobManager.stop();
    }

    private void setPartition(String partitionInterval) {
        String[] split = partitionInterval.split(EJConstants.SPLIT_STR);
        if (split == null || split.length != 2) {
            log.info("任务区间格式有误:{}", partitionInterval);
            return;
        }
        try {
            int b = Integer.parseInt(split[0]);
            int e = Integer.parseInt(split[1]);
            jobManager.restart(b,e);
        } catch (NumberFormatException ex) {
            log.info("任务区间格式有误:{}", partitionInterval);
        }

    }
}
