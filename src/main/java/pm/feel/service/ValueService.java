package pm.feel.service;

import com.google.common.base.Splitter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pm.feel.model.VoteResponse;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stores values of
 */
@Service
@Slf4j
public class ValueService implements InitializingBean {
    @Autowired MeterRegistry registry;  //prometheus gauges

    Map<String, Set<Long>> upVotes;
    Map<String, Set<Long>> dnVotes;
    Map<String, AtomicInteger> upVotesInPeriod;
    Map<String, AtomicInteger> dnVotesInPeriod;
    Set<String> rooms;

    @Value("${rooms}")
    String roomConfig;
    @Value("${duration.persist.votes}") //default=1h
    Long durationPersistVotes;

    @Override
    public void afterPropertiesSet() {
        upVotes = new ConcurrentHashMap<>();
        dnVotes = new ConcurrentHashMap<>();
        upVotesInPeriod = new ConcurrentHashMap<>();
        dnVotesInPeriod = new ConcurrentHashMap<>();

        rooms = new HashSet<>(Splitter.on(",").trimResults().omitEmptyStrings().splitToList(roomConfig));

        rooms.forEach(room -> {
            AtomicInteger up = registry.gauge(room + "_up", new AtomicInteger(0));
            AtomicInteger dn = registry.gauge(room + "_dn", new AtomicInteger(0));
            upVotesInPeriod.put(room, up);
            dnVotesInPeriod.put(room, dn);
            Set<Long> u = new ConcurrentSkipListSet<>(), d = new ConcurrentSkipListSet<>();
            upVotes.put(room, u);
            dnVotes.put(room, d);
        });
    }

    public VoteResponse vote(String room, int val){
        if (!upVotes.keySet().contains(room)) return new VoteResponse(-1,-1);
        long now = new Date().getTime();
        if (val==1) {
            upVotes.get(room).add(now);
        } else if (val==-1) {
            dnVotes.get(room).add(now);
        }
        sweepOutOldTimestamps();
        //counters/gauges will be updated by scheduler
        return new VoteResponse(upVotesInPeriod.get(room).get(), dnVotesInPeriod.get(room).get());
    }

    @Scheduled(cron = "0/30 * * * * ?")
    public void sweepOutOldTimestamps() {
        log.info("Sweeping-out old votes");
        long now = new Date().getTime();

        rooms.forEach(room->{
            final Set<Long> toremove = new HashSet<>();
            upVotes.get(room).forEach(timestamp -> {
                if (now - timestamp > durationPersistVotes) toremove.add(timestamp);
            });
            upVotes.get(room).removeAll(toremove);
            toremove.clear();
            dnVotes.get(room).forEach(timestamp -> {
                if (now - timestamp > durationPersistVotes) toremove.add(timestamp);
            });
            dnVotes.get(room).removeAll(toremove);
            ///
            upVotesInPeriod.get(room).set(upVotes.get(room).size());
            dnVotesInPeriod.get(room).set(dnVotes.get(room).size());
        });
    }

    public Set<String> getRooms() {
        return rooms;
    }
}
