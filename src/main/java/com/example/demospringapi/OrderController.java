package com.example.demospringapi;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.vnderrors.VndErrors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.parameters.RequestBody;

@RestController
class OrderController {
    
    private final OrderRepository repository;
    private final OrderModelAssembler assembler;

    OrderController(OrderRepository repository, OrderModelAssembler assembler) {
        this.repository = repository;
        this.assembler = assembler;
    }

    @GetMapping("/orders")
    CollectionModel<EntityModel<Order>> all() {
        List<EntityModel<Order>> orders = repository.findAll().stream()
            .map(assembler::toModel)
            .collect(Collectors.toList());

        return new CollectionModel<>(orders,
            linkTo(methodOn(OrderController.class).all()).withSelfRel());
    }

    @GetMapping("/orders/{id}")
    EntityModel<Order> one(@PathVariable Long id) {
        Order order = repository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));

        return assembler.toModel(order);
    }

    @PostMapping("/orders")
    ResponseEntity<EntityModel<Order>> newOrder(@RequestBody Order order) {
        order.setStatus(Status.IN_PROGRESS);
        Order newOrder = repository.save(order);

        return ResponseEntity
            .created(linkTo(methodOn(OrderController.class).one(newOrder.getId())).toUri())
            .body(assembler.toModel(newOrder));
    }

    @DeleteMapping("/orders/{id}/cancel")
    public ResponseEntity<RepresentationModel> cancel(@PathVariable Long id) {
        Order order = repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() == Status.IN_PROGRESS) {
            order.setStatus(Status.CANCELLED);
            return ResponseEntity.ok(assembler.toModel(repository.save(order)));
        }

        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(new VndErrors.VndError("Method not allowed", "You can't cancel an order that is in the " + order.getStatus() + " status"));
    }

    @PutMapping("/orders/{id}/complete")
    public ResponseEntity<RepresentationModel> complete(@PathVariable Long id) {
        Order order = repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() == Status.IN_PROGRESS) {
            order.setStatus(Status.COMPLETED);
            return ResponseEntity.ok(assembler.toModel(repository.save(order)));
        }

        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(new VndErrors.VndError("Method not allowed", "You can't complete an order that is in the " + order.getStatus() + " status"));
    }
}