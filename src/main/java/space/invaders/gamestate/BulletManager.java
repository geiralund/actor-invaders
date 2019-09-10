package space.invaders.gamestate;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import space.invaders.dto.BulletDto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BulletManager extends AbstractActor {
    private int nextId = 1;
    private Map<ActorRef, BulletDto> refToBullet = new HashMap<>();

    static Props props() {
        return Props.create(BulletManager.class, BulletManager::new);
    }

    public static class CreateBullet {

        public int posX;
        public int posY;

        public CreateBullet(int posX, int posY) {
            this.posX = posX;
            this.posY = posY;
        }

    }

    public static class Update {
        public List<BulletDto> bulletDtoList;

        public Update(Collection<BulletDto> bulletDtoList) {
            this.bulletDtoList = Collections.unmodifiableList(new ArrayList<>(bulletDtoList));
        }
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CreateBullet.class, createBullet ->
                {
                    String name = getSender().path().name();
                    if(name.equals("player")){
                        create(createBullet,BulletDto.Sender.Player);

                    } else {
                        create(createBullet,BulletDto.Sender.Alien);
                    }


                })
                .match(Game.Tick.class, tick -> {
                    refToBullet.keySet().forEach(actorRef -> actorRef.forward(tick, getContext()));
                    getContext().parent().tell(new Update(refToBullet.values()), getSelf());
                })
                .match(Bullet.Update.class, update -> {




                    refToBullet.put(getSender(), update.bulletDto);
                })
                .match(Terminated.class, terminated -> {
                    refToBullet.remove(terminated.getActor());
                }).build();
    }

    private void create(CreateBullet createBullet, BulletDto.Sender sender) {
        final int id = nextId++;
        final ActorRef key = getContext().actorOf(
                Bullet.props(id, createBullet.posX, createBullet.posY),
                "bullet" + id);
        getContext().watch(key);
        refToBullet.put(
                key,
                new BulletDto(id, createBullet.posX, createBullet.posY, sender));
    }
}