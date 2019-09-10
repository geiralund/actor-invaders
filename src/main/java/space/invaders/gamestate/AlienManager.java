package space.invaders.gamestate;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import space.invaders.dto.AlienDto;

import java.util.*;
import java.util.stream.IntStream;

public class AlienManager extends AbstractActor {
    private Map<ActorRef, AlienDto> refToAlien = new HashMap<>();
    private final int columns = 10;
    private final int rows = 4;
    private ActorRef [][] alienGrid = new ActorRef [rows][columns];
    private ActorRef bulletManager;

    public AlienManager(ActorRef bulletManager){
        this.bulletManager = bulletManager;
        IntStream.range(0, columns).forEach(col  ->
                IntStream.range(0, rows).forEach( row ->
                    create(col, row))

        );
    }


    static Props props(ActorRef bulletManager) {
        return Props.create(AlienManager.class, () -> new AlienManager(bulletManager));
    }

    public static class Update {
        public List<AlienDto> alienDtoList;

        public Update(Collection<AlienDto> alienDtos) {
            this.alienDtoList = Collections.unmodifiableList(new ArrayList<>(alienDtos));
        }
    }


    @Override
    public Receive createReceive() {
        return
                receiveBuilder()
                        .match(AlienDto.class, alienDto -> {
                            refToAlien.put(getSender(), alienDto);

                        })
                        .match(Game.Tick.class, tick -> {
                            refToAlien.keySet().forEach(actorRef -> actorRef.forward(tick, getContext()));
                            bulletFire();
                            getContext().parent().tell(new AlienManager.Update(refToAlien.values()), getSelf());

                        })
                        .match(Terminated.class, terminated -> {
                            refToAlien.remove(terminated.getActor());
                        })
                        .build();
    }

    private void bulletFire() {
        Random random = new Random();
        int shoot = random.nextInt(100);
        if(shoot <50){
            ActorRef alien = null;
            do{
                int col = random.nextInt(columns);
                for (int i = alienGrid.length-1; i >= 0; i--) {
                    if(alienGrid[i][col] != null){
                        alien = alienGrid[i][col];
                        break;

                    }
                }
                          }
            while (alien == null);
            alien.tell(new Alien.Fire(bulletManager), getSelf());
        }

    }

    private void create(int col, int row) {
        int intPosX = 60 * col;
        int intPosY = 60 * row;
        final int id = Integer.valueOf(""+ col + row);
        AlienImageSet alienImageSet = AlienImageSet.images.get(row % 3);
        final ActorRef key = getContext().actorOf(
                Alien.props(id, intPosX, intPosY, alienImageSet),
                "alien" + id);
        refToAlien.put(key, new AlienDto(id, intPosX, intPosY, alienImageSet.getFirst()));
        getContext().watch(key);
        alienGrid[row][col] = key;
    }
}
