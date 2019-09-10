package space.invaders.gamestate;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import space.invaders.dto.AlienDto;
import space.invaders.dto.BulletDto;
import space.invaders.dto.Image;

public class Alien extends AbstractActor {

    private final int id;
    private final int posX;
    private final int posY;
    private AlienImageSet alienImageSet;
    private Image current;


    public Alien(int id, int posX, int posY, AlienImageSet alienImageSet) {
        this.id = id;
        this.posX = posX;
        this.posY = posY;
        this.alienImageSet = alienImageSet;
        this.current = alienImageSet.getFirst();
    }


    public static Props props(int id, int posX, int posY, AlienImageSet alienImageSet) {
        return Props.create(Alien.class, () -> new Alien(id, posX, posY, alienImageSet));
    }


    public static class Fire {

        public final ActorRef bulletManager;


        public Fire(ActorRef bulletManager) {
            this.bulletManager = bulletManager;
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Game.Tick.class, tick -> {
                    getContext().parent().tell(
                            new AlienDto(
                                    id, posX, posY, current
                            ), getSelf()
                    );
                })
                .match(Alien.Fire.class, fire -> {
                    fire.bulletManager.tell(new BulletManager.CreateBullet((posX+alienImageSet.getWidth()/2), posY+alienImageSet.getHeight()/2, BulletDto.Sender.Alien), getSelf());
                })
                .build();
    }
}
