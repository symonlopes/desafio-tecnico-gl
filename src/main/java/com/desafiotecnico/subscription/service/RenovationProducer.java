package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.dto.event.RenovationEvent;

public interface RenovationProducer {
    void sendRenovationStart(RenovationEvent event);
}
