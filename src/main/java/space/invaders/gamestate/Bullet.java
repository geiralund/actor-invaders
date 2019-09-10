package space.invaders.gamestate;

import akka.actor.AbstractActor;
import akka.actor.Props;
import space.invaders.dto.BulletDto;
import space.invaders.dto.GameStateDto;

public class Bullet extends AbstractActor {

    private final int id;
    private int posX;
    private int posY;

    private static final int sceneWidth = GameStateDto.screenSize.width;
    private static final int sceneHeight = GameStateDto.screenSize.height;

    public Bullet(int id, int posX, int posY) {
        this.id = id;
        this.posX = posX;
        this.posY = posY;
        getContext().parent().tell(new Update(generateBullet()), getSelf());
    }

    static Props props(int id, int posX, int posY) {
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
        posY = posY - 10;

        getContext().parent().tell(
                new Bullet.Update(generateBullet()),
                getSelf());

        if (sceneHeight < posY || posY < 0) {
            stop();
        }

    }

    private void stop() {
        getContext().stop(getSelf());
    }

    private BulletDto generateBullet() {
        return new BulletDto(this.id, posX, posY, BulletDto.Sender.Player);
    }
}

