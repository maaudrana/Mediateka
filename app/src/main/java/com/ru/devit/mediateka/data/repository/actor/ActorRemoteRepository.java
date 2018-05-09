package com.ru.devit.mediateka.data.repository.actor;

import com.ru.devit.mediateka.data.datasource.db.CinemaActorJoinDao;
import com.ru.devit.mediateka.data.datasource.network.CinemaApiService;
import com.ru.devit.mediateka.models.db.CinemaActorJoinEntity;
import com.ru.devit.mediateka.models.mapper.ActorDetailEntityToActor;
import com.ru.devit.mediateka.models.mapper.ActorDetailResponseToActor;
import com.ru.devit.mediateka.models.model.Actor;
import com.ru.devit.mediateka.models.model.Cinema;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Flowable;
import io.reactivex.Single;

public class ActorRemoteRepository implements ActorRepository {

    private final CinemaApiService apiService;
    private final ActorDetailResponseToActor networkMapper;
    private final ActorDetailEntityToActor dbMapper;
    private final ActorLocalRepository cache;
    private final CinemaActorJoinDao cinemaActorJoinDao;

    public ActorRemoteRepository(CinemaApiService apiService,
                                         ActorDetailResponseToActor networkMapper,
                                         ActorDetailEntityToActor dbMapper ,
                                         ActorLocalRepository localRepository ,
                                         CinemaActorJoinDao cinemaActorJoinDao) {
        this.apiService = apiService;
        this.networkMapper = networkMapper;
        this.dbMapper = dbMapper;
        this.cache = localRepository;
        this.cinemaActorJoinDao = cinemaActorJoinDao;
    }

    @Override
    public Flowable<List<Actor>> searchActors(String query) {
        return apiService.searchActors(query)
                .map(networkMapper::map)
                .doAfterNext(actors -> cache.insertActors(dbMapper.map(actors)))
                .onErrorResumeNext(throwable -> {
                    return cache.searchActors("%" + query + "%");
                });
    }

    @Override
    public Single<Actor> getActorById(final int actorId) {
        return apiService.getActorById(actorId , "tagged_images,movie_credits")
                .map(networkMapper::map)
                .doAfterSuccess(actor -> {
                    cache.updateActor(actorId ,
                            actor.getBiography() ,
                            actor.getBirthDay() ,
                            actor.getAge() ,
                            actor.getPlaceOfBirth());
                    cache.insertCinemasForActor(dbMapper.mapCinemas(actor.getCinemas()));
                    createRelationBetweenActorAndCinemas(actorId , actor.getCinemas());
                })
                .onErrorResumeNext(throwable -> cache.getActorById(actorId));
    }

    private void createRelationBetweenActorAndCinemas(final int actorId , List<Cinema> cinemas){
        for (Cinema cinema : cinemas){
            cinemaActorJoinDao.insert(new CinemaActorJoinEntity(cinema.getId() , actorId));
        }
    }
}
