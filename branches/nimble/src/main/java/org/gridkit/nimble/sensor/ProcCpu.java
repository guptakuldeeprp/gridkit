package org.gridkit.nimble.sensor;

public class ProcCpu {
    private long usr = 0;
    private long sys = 0;
    private long tot = 0;
    private long cnt = 0;
    
    public ProcCpu add(org.hyperic.sigar.ProcCpu procCpu) {
        usr += procCpu.getUser();
        sys += procCpu.getSys();
        tot += procCpu.getTotal();
        cnt += 1;
        return this;
    }

    public long getUsr() {
        return usr;
    }

    public long getSys() {
        return sys;
    }

    public long getTot() {
        return tot;
    }
    
    public long getCnt() {
        return cnt;
    }
}