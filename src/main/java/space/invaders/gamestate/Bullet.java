package space.invaders.gamestate;

import akka.actor.AbstractActor;
import akka.actor.Props;
import space.invaders.dto.BulletDto;

public class Bullet extends AbstractActor {
    private final int id;
    private int posX;
    private int posY;


    public Bullet(int id, int posX, int posY) {
        this.id = id;
        this.posX = posX;
        this.posY = posY;
    }

    static Props props(int id, int posX, int posY){
        return Props.create(Bullet.class, () -> new Bullet(id, posX, posY));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Game.Tick.class, tick -> {
                    updateBullet();
                })
                .build();
    }

    public static class Update {
        public final BulletDto bulletDto;


        public Update(BulletDto bulletDto) {
            this.bulletDto = bulletDto;
        }
    }

    private void updateBullet() {
            posY = posY + 10;
            getContext().parent().tell(new Bullet.Update(
                    new BulletDto(this.id, posX, posY, BulletDto.Sender.Player)
            ), getSelf());

    }
}

