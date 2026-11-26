package com.nanaios.fieldtheory.core;

import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.charge.EntityCharges;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class FieldAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FieldTheoryAPI.MOD_ID);

    /** Induced charges of an entity. */
    public static final Supplier<AttachmentType<EntityCharges>> ENTITY_CHARGES = REGISTER.register("entity_charges",
            () -> AttachmentType.builder(() -> EntityCharges.EMPTY)
                    .serialize(EntityCharges.CODEC, charges -> !charges.isEmpty())
                    .build());

    private FieldAttachments() {
    }
}
