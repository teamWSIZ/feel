package pm.feel.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import pm.feel.model.GenericResponse;
import pm.feel.model.VoteResponse;
import pm.feel.service.ValueService;

/**
 * Allows unauthenticated votes up or down for each of the pre-set rooms.
 */

@RestController
@CrossOrigin
@Slf4j
public class FeelController {
    @Autowired ValueService valueService;

    @GetMapping(value = "/status")
    public GenericResponse getStatus() {
        return new GenericResponse("Feel app running OK");
    }

    @GetMapping(value = "/rooms")
    public Iterable<String> getRooms() {
        return valueService.getRooms();
    }

    //val == {1,-1}
    @GetMapping(value = "/vote/{room}/{val}")
    public VoteResponse getRooms(
            @PathVariable(name = "room") String room,
            @PathVariable(name = "val") Integer vote
    ) {
        return valueService.vote(room, vote);
    }


}
