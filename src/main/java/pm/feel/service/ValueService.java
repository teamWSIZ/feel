package pm.feel.service;

import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pm.feel.model.Device;
import pm.feel.repo.DeviceRepo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Stores values of all services in a way exposed to prometheus.
 */
@Service
@Slf4j
public class ValueService {
    @Autowired MeterRegistry registry;  //prometheus gauges
    @Autowired DeviceRepo deviceRepo;

    /*
     * gauge_name -> last value ;
     * on startup gauges are recreated on each new update ;
     * DB does not store gauge values, only names of devices and sensors ;
     */
    Map<String, AtomicDouble> values = new ConcurrentHashMap<>();

    //non-null names are written to the device description

    /**
     * Makes sure device given by the MAC address is stored in the DB (of names of devices and sensors).
     *
     */
    public Device identify(String mac, String name, String nameA, String nameB, String nameC) {
        log.info("Device update request for mac=[{}], [{}], [{}], [{}], [{}]", mac, name, nameA, nameB, nameC);
        if (mac==null) throw new RuntimeException("Identifying device while giving MAC=null");
        Device d = deviceRepo.findByMacAddress(mac);
        if (d==null) {
            if (name==null) throw new RuntimeException("New device (MAC); `name` should not be null");
            d = Device.default_(mac);
        }
        d.setName(name);
        if (nameA!=null) d.setNameA(nameA);
        if (nameB!=null) d.setNameB(nameB);
        if (nameC!=null) d.setNameC(nameC);
        deviceRepo.save(d);
        log.info("Device identification updated: [{}]", d);
        return d;
    }


    /**
     * Functions:
     * - mac not in DB --> register (and exit)
     * - mac in DB, but not named --> exit
     * - mac in DB, named -> store values (creating gauges if necessary)
     */
    public void update(String mac, Double valA, Double valB, Double valC) {
        log.info("Update of [{}], with [{}], [{}], [{}]", mac, valA, valB, valC);
        if (isNullOrEmpty(mac)) return;

        Device d = deviceRepo.findByMacAddress(mac);
        if (d==null) {
            d = deviceRepo.save(Device.default_(mac)); //new registration; not yet identified
            return;
        }
        String name = d.getName();
        if (isNullOrEmpty(name)) return;
        log.info("Device identified as [{}], storing values in gauges", name);

        storeValue(gaugeName(d.getName(), d.getNameA()), valA);
        storeValue(gaugeName(d.getName(), d.getNameB()), valB);
        storeValue(gaugeName(d.getName(), d.getNameC()), valC);
    }

    /**
     * Updates or creates&updates a gauge.
     */
    private void storeValue(String gaugeName, Double value) {
        if (values.containsKey(gaugeName)) {
            values.get(gaugeName).set(value);
        } else {
            AtomicDouble gauge = registry.gauge(gaugeName, new AtomicDouble(value));
            values.put(gaugeName, gauge);
        }
        log.info("[{}] updated to [{}]", gaugeName, value);
    }

    private String gaugeName(String deviceName, String sensorName) {
        return deviceName + "_" + sensorName;
    }

    /**
     * - delete from DB
     * - delete all gauges whose names start with "devicename_"
     */
    @Transactional
    public void delete(String mac) {
        log.warn("Deleting device with mac=[{}]", mac);
        Device d = deviceRepo.findByMacAddress(mac);
        if (d==null || d.getName()==null) return;
        String prefix = d.getName() + "_";
        Set<String> todelete = new HashSet<>();
        for(String gaugeName : values.keySet()) {
            if (gaugeName.startsWith(prefix)) todelete.add(gaugeName);
        }
        todelete.forEach(s -> {
            log.warn("Removing gauge [{}]", s);
            values.remove(s);
        });
        deviceRepo.delete(d);
        log.warn("Device deleted ");
        cleanup();
    }

    private void cleanup() {
        log.warn("Cleaning up (removing) all devices with MAC=null");
        deviceRepo.deleteByMacAddressIsNull();
    }
}
