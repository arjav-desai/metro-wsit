/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.xml.ws.rx.rm.runtime.sequence;

import com.sun.xml.ws.commons.Logger;
import com.sun.xml.ws.rx.rm.faults.AbstractSoapFaultException;
import com.sun.xml.ws.rx.rm.faults.AbstractSoapFaultException.Code;
import com.sun.xml.ws.rx.rm.runtime.ApplicationMessage;
import com.sun.xml.ws.rx.rm.runtime.delivery.DeliveryQueueBuilder;
import com.sun.xml.ws.rx.rm.runtime.sequence.Sequence.AckRange;
import com.sun.xml.ws.rx.util.TimeSynchronizer;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * TODO javadoc
 * TODO make class thread-safe
 * @author Marek Potociar (marek.potociar at sun.com)
 */
public final class OutboundSequence extends AbstractSequence {
    public static final long INITIAL_LAST_MESSAGE_ID = Sequence.MIN_MESSAGE_ID - 1;

    private static final Logger LOGGER = Logger.getLogger(OutboundSequence.class);
    //
    private final List<Long> unackedMessageIdentifiers;

    public OutboundSequence(SequenceData data, DeliveryQueueBuilder deliveryQueueBuilder, TimeSynchronizer timeSynchronizer) {
        super(data, deliveryQueueBuilder, timeSynchronizer);

        this.unackedMessageIdentifiers = new LinkedList<Long>();
    }

    @Override
    Collection<Long> getUnackedMessageIdStorage() {
        return unackedMessageIdentifiers;
    }

    public void registerMessage(ApplicationMessage message, boolean storeMessageFlag) throws DuplicateMessageRegistrationException, AbstractSoapFaultException {
        checkSequenceCreatedStatus("", Code.Sender); // TODO

        if (message.getSequenceId() != null) {
            throw new IllegalArgumentException(String.format(
                    "Cannot register message: Application message has been already registered on a sequence [ %s ].",
                    message.getSequenceId()));
        }

        try {
            data.lockWrite();

            message.setSequenceData(this.getId(), generateNextMessageId());
            if (storeMessageFlag) {
                storeMessage(message, getUnackedMessageIdentifierKey(message.getMessageNumber()));
            }
        } finally {
            data.unlockWrite();
        }
    }

    private long generateNextMessageId() throws MessageNumberRolloverException, IllegalStateException {
        // no need to synchronize, called from within a write lock

        long nextId = getLastMessageId() + 1;
        if (nextId > Sequence.MAX_MESSAGE_ID) {
            throw LOGGER.logSevereException(new MessageNumberRolloverException(getId(), nextId));
        }

        data.setLastMessageId(nextId);

        // Making sure we have a new, uncached long object which GC can dispose later - used in storeMessage()
        // WARNING: this call to new Long(...) CANNOT be replaced with Long.valueOf(...) !!!
        unackedMessageIdentifiers.add(new Long(nextId));
        return nextId;
    }

    private Long getUnackedMessageIdentifierKey(long messageNumber) {
        Long msgNumberKey = null;
        int index = unackedMessageIdentifiers.indexOf(messageNumber);
        if (index >= 0) {
            // the id is in the list
            msgNumberKey = unackedMessageIdentifiers.get(index);
        }
        return msgNumberKey;
    }

    public void acknowledgeMessageId(long messageId) {
        // TODO L10N
        throw new UnsupportedOperationException(String.format("This operation is not supported on %s class", this.getClass().getName()));
    }

    public void acknowledgeMessageIds(List<AckRange> ranges) throws InvalidAcknowledgementException, AbstractSoapFaultException {
        checkSequenceCreatedStatus("", Code.Sender); // TODO

        try {
            data.lockWrite();

            if (ranges == null || ranges.isEmpty()) {
                return;
            }

            if (ranges.size() > 1) {
                Collections.sort(ranges, new Comparator<AckRange>() {

                    public int compare(AckRange range1, AckRange range2) {
                        if (range1.lower <= range2.lower) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                });
            }

            // check proper bounds of acked ranges
            AckRange lastAckRange = ranges.get(ranges.size() - 1);
            if (getLastMessageId() < lastAckRange.upper) {
                throw new InvalidAcknowledgementException(this.getId(), lastAckRange.upper, ranges);
            }

            if (unackedMessageIdentifiers.isEmpty()) {
                // we have checked the ranges are ok and there's nothing to acknowledge.
                return;
            }

            // acknowledge messages
            Iterator<Long> unackedIterator = unackedMessageIdentifiers.iterator();
            Iterator<AckRange> rangeIterator = ranges.iterator();
            AckRange currentRange = rangeIterator.next();
            while (unackedIterator.hasNext()) {
                long unackedIndex = unackedIterator.next();
                if (unackedIndex >= currentRange.lower && unackedIndex <= currentRange.upper) {
                    unackedIterator.remove();
                } else if (rangeIterator.hasNext()) {
                    currentRange = rangeIterator.next();
                } else {
                    break; // no more acked ranges
                }
            }
        } finally {
            data.unlockWrite();
        }
        
        this.getDeliveryQueue().onSequenceAcknowledgement();
    }
}
