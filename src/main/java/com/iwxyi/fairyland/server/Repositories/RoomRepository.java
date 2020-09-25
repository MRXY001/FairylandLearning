package com.iwxyi.fairyland.server.Repositories;

import java.util.List;

import com.iwxyi.fairyland.server.Models.Room;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

public interface RoomRepository extends CrudRepository<Room, Long> {
    Room findByRoomId(Long roomId);

    List<Room> findByOwnerIdAndDeletedFalse(Long ownerId);

    Page<Room> findByDeletedFalse(Pageable pageable);

}