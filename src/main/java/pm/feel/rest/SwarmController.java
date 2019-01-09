package pm.feel.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pm.feel.model.Device;
import pm.feel.model.GenericResponse;
import pm.feel.repo.DeviceRepo;
import pm.feel.service.ValueService;

/**
 * - agents register to swarm_ctrl (10.10.0.13).   "/register" (mainly MAC, IP --> id)
 * - agents submit updates (mac -> vA, vB, vC) "/updates"
 * - user adds desciption to agents, and to vA, vB, vC
 * - agents should download ssid settings from swarm_ctrl "/settings"
 *
 * - swarm_ctrl should expose (prometheus):
 *   deviceIdentity_sensorA, deviceIdentity_sensorB, deviceIdentity_sensorC
 *
 */


@RestController
@CrossOrigin
@Slf4j
public class SwarmController {
    @Autowired DeviceRepo deviceRepo;
    @Autowired ValueService valueService;


    @GetMapping(value = "/devices")
    public Iterable<Device> getDevices() {
        return deviceRepo.findAll();
    }

    @GetMapping(value = "/updates")
    public GenericResponse update(
            @RequestParam(value = "mac") String mac,
            @RequestParam(value = "vA") Double valA,
            @RequestParam(value = "vB") Double valB,
            @RequestParam(value = "vC") Double valC){
        valueService.update(mac, valA, valB, valC);
        return new GenericResponse("OK");
    }

    @GetMapping(value = "/identify")
    public Device identify(
            @RequestParam(value = "mac") String mac,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "nameA", required = false) String nameA,
            @RequestParam(value = "nameB", required = false) String nameB,
            @RequestParam(value = "nameC", required = false) String nameC
            ) {
        return valueService.identify(mac, name, nameA, nameB, nameC);
    }

    @DeleteMapping(value = "/identify")
    public GenericResponse delete(@RequestParam(value = "mac")String mac) {
        valueService.delete(mac);
        return new GenericResponse("OK");
    }




}
