package com.result.sim;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;

public class ResultTrafficSimulation {

    private static final int NUM_USERS = 100;
    private static final int INITIAL_VMS = 2;
    private static final int MAX_VMS = 10;
    private static final int HOST_PES = 4;  // 4 cores per host
    private static final int VM_PES = 2;    // 2 cores per VM
    private static final int HOST_MIPS = 10000; // 10000 MIPS per core
    private static final int CLOUDLET_LENGTH = 1000; // Reduced from 5000

    private static DatacenterBrokerSimple broker;
    private static List<Vm> vmList;
    private static List<Cloudlet> cloudletList;
    private static CloudSim simulation;

    public static void main(String[] args) {
        simulation = new CloudSim();
        simulation.terminateAt(30); // Increased to 30 seconds

        Datacenter datacenter = createDatacenter(simulation);
        broker = new DatacenterBrokerSimple(simulation);

        vmList = createVmList(INITIAL_VMS);
        boolean scaled = monitorAndScale();
        broker.submitVmList(vmList);

        cloudletList = createCloudletList();
        broker.submitCloudletList(cloudletList);

        simulation.start();

        List<Cloudlet> finished = broker.getCloudletFinishedList();
        printResults(finished, scaled);
    }

    private static Datacenter createDatacenter(CloudSim simulation) {
        List<Host> hostList = new ArrayList<>();
        // Create 5 powerful hosts
        for (int i = 0; i < 5; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int j = 0; j < HOST_PES; j++) {
                peList.add(new PeSimple(HOST_MIPS, new PeProvisionerSimple()));
            }
            // 32GB RAM, 10Gbps bandwidth, 1TB storage
            Host host = new HostSimple(32768, 100000, 1000000, peList);
            hostList.add(host);
        }
        return new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
    }

    private static List<Vm> createVmList(int count) {
        List<Vm> vms = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // VMs with 2 cores and TimeShared scheduler
            Vm vm = new VmSimple(HOST_MIPS, VM_PES)
                .setRam(8192)       // 8GB RAM
                .setBw(10000)        // 10Gbps bandwidth
                .setSize(10000)      // 10GB storage
                .setCloudletScheduler(new CloudletSchedulerTimeShared());
            vms.add(vm);
        }
        return vms;
    }

    private static List<Cloudlet> createCloudletList() {
        List<Cloudlet> list = new ArrayList<>();
        for (int i = 0; i < NUM_USERS; i++) {
            Cloudlet cloudlet = new CloudletSimple(CLOUDLET_LENGTH, 1);
            cloudlet.setUtilizationModel(new UtilizationModelFull());
            list.add(cloudlet);
        }
        return list;
    }

    private static boolean monitorAndScale() {
        int expectedPerVm = NUM_USERS / INITIAL_VMS;
        if (expectedPerVm > 20) {
            System.out.println("⚠️ High load detected: Spinning up extra VMs...");
            int additional = Math.min(MAX_VMS - INITIAL_VMS, 3);
            List<Vm> newVms = createVmList(additional);
            vmList.addAll(newVms);
            System.out.println("✅ " + additional + " VMs added.");
            return true;
        }
        return false;
    }
    

    private static void printResults(List<Cloudlet> finished, boolean scaled) {
        System.out.println("\n========================= ✅ SIMULATION RESULTS =========================");
        System.out.printf("🖥️ Total VMs Used: %d\n", vmList.size());
        System.out.println(scaled ? "📈 Auto-scaling Triggered: Extra VMs were added!" 
                                : "🟢 Load handled with initial VM resources.");

        if (finished.isEmpty()) {
            System.out.println("\n❌ CRITICAL: No cloudlets completed. Possible issues:");
            System.out.println(" - Check if cloudlet length is too high for VM capacity");
            System.out.println(" - Verify VM scheduling policy (using TimeShared)");
            System.out.println(" - Ensure sufficient simulation time (currently 30 seconds)");
            System.out.println(" - Check host PE allocation and MIPS capacity");
        } else {
            System.out.printf("\n📝 Completed %d of %d cloudlets (%.1f%%)\n", 
                finished.size(), NUM_USERS, (finished.size()*100.0/NUM_USERS));
            
            System.out.println("\n📊 Performance Metrics:");
            double totalTime = finished.stream().mapToDouble(Cloudlet::getActualCpuTime).sum();
            double avgTime = totalTime / finished.size();
            System.out.printf(" - Average completion time: %.2f sec\n", avgTime);
            
            System.out.println("\n🔍 Sample Completed Cloudlets:");
            finished.stream().limit(5).forEach(cl -> System.out.printf(
                " - Cloudlet %d (VM %d): %.2f sec\n",
                cl.getId(), cl.getVm().getId(), cl.getActualCpuTime()));
            
            if (finished.size() > 5) {
                System.out.println("...and " + (finished.size() - 5) + " more");
            }
        }
        System.out.println("========================================================================\n");
    }
}