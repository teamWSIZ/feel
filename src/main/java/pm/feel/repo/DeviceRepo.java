package pm.feel.repo;

import org.springframework.data.repository.CrudRepository;
import pm.feel.model.Device;

public interface DeviceRepo extends CrudRepository<Device, Integer> {
    Device findByMacAddress(String mac);
    void deleteByMacAddressIsNull();
}
