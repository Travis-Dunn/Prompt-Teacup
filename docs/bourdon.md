# Bourdon

Dev reference. Does not ship. The point of this document is to keep the Bourdon consistent with the rest of the world as the world gets built. Anything marked **open** has not been decided, and should not be treated as canon until it is.

| | |
|---|---|
| Name | Bourdon (French: bumblebee) |
| Official designation | CGF Type 4 |
| Full name, as printed in the shipyard | Ferrand Type 4 Bourdon |
| Manufacturer | Compagnie Générale Ferrand (CGF, "Ferrand"), exclusively |
| Role | Light freighter. The player's starting ship. |
| Crew | One. No passengers, ever. |
| Range | One jump. The shortest range of any ship in the game. |
| Weapons | One small defensive blaster turret, on top. No other mounts. |
| Stock livery | Yellow body, black shield generator, bare aluminum struts, amber lights |

## Summary

The Bourdon is the ship that brought super-luminal travel to the masses. It is a cheap, old design that has been in production, essentially unchanged, for about fifty years. When it was introduced it was a compelling, well rounded ship. Today it barely checks the minimum number of boxes. It is the most humble ship in the game.

## The manufacturer

Compagnie Générale Ferrand is a French conglomerate, in the mould of the big state-adjacent French industrial groups of the 1960s and 70s: it makes a bit of everything, and the Bourdon is only one of its product lines. CGF is the sole manufacturer of the Bourdon and always has been. Licensed production, if any, is **open**.

The name "Bourdon" comes from the colours of the stock livery - yellow and black - and from the low drone of its engines. The old pilot's joke that a bumblebee shouldn't be able to fly applies.

## History

- Introduced roughly fifty years before the game begins. Exact dates are **open**.
- At the time, super-luminal drives and shield generators were both rare on civilian craft. The Bourdon was one of the first ships to put both within reach of ordinary people, and it did so by being ruthlessly economical about everything else.
- The shield covers only the sides and rear. That was a cost decision: shield generators were expensive, and a freighter is supposed to run from a fight, not face it.
- One jump of range was normal, or close to it, when the Bourdon was new. Today even regional ships manage two or three, and one jump is almost comical.
- There is a wide array of bolt-on upgrades for it. A few are official CGF parts. Most are not. Which upgrades exist is **open**.

## Design philosophy

The Bourdon was designed when engineers had to respect physics in ways that contemporary ship designers seem not to. The shape follows from that:

- Only the pilot needs pressure, so only the pilot gets it. The cargo hold is unpressurized.
- The cheapest, lightest pressure vessel is a sphere, so the crew cabin is a sphere.
- Anything that doesn't need pressure lives outside the sphere.
- There is no passage between the cabin and the hold, so the cabin doesn't need to be joined to the hold by anything more than a few aluminum struts.

It should look like an honest piece of 1970s engineering, not a pile of greebles.

## Layout

Top view, nose up, not to scale.

```
              ___
            /▓    \  oo ▮       crew sphere: canopy on the left, equipment on the right
            \_____/
            /  |  \             3-4 struts across an open gap
        ┌─────────────┐         front cap / cargo door, hinged on the right
      █ │             │ █
      █ │    (T)      │ █       cargo cylinder, turret on top
      █ │             │ █
      █ └─────────────┘ █
      ███████████████████       black U-shaped shield generator, sides and rear
           ═══════              upper impulse engine (the lower one is hidden beneath)
```

### Crew sphere

- One pilot. Left-hand drive, so the pilot sits on the left (port) side.
- The canopy is a smooth, flush bubble on the pilot's side only. Not faceted. The rest of the sphere is opaque yellow.
- Strapped to the outside of the right (starboard) side: spherical gas bottles, a flat radiator panel, and an amber beacon. This is what keeps the silhouette asymmetrical.
- The sphere is self-contained, and doubles as a lifeboat. It can detach from the rest of the ship entirely. It carries life support and the beacon. Whether it can manoeuvre on its own is **open**.

### Struts and the gap

- Three or four straight, bare aluminum struts run from the front cap of the cylinder to the sphere. No lattice truss.
- A single cable bundle runs along one strut, carrying power and data to the rest of the ship. It is simply cut when the sphere ejects.
- The gap between sphere and cylinder is open space. It must be clearly visible from the gameplay camera, with stars showing through it.
- On ejection, the sphere releases at the top of the struts. The struts stay with the hull.

### Front cap and cargo door

- The front cap of the cargo cylinder is the cargo door. It is hinged on the right.
- The struts and sphere are mounted to the cap, so the whole assembly - cap, struts, and sphere with the pilot in it - swings aside to open the hold, like the nose of a Super Guppy.

### Cargo cylinder

- Unpressurized. Carries a small but meaningful amount of cargo.
- The super-luminal drive is somewhere in the hull, and is never shown. Its workings are never explained.
- The defensive blaster turret sits on top of the cylinder.

### Shield generator

- A U-shaped band wrapping around the sides and rear of the cylinder at its widest point.
- Black, styled after the "diving board" rubber bumper on North American market BMW E30s. It should read as a bumper first.
- Does not cover the front.

### Impulse engines

- Two small, horizontal, linear engines at the aft end. One sits above the shield generator, one below.
- They are small, and the ship is sluggish.
- Exhaust glow is amber.

## Appearance

- Stock livery is yellow, with the black shield generator, bare aluminum struts, dark canopy glass, and amber lights.
- The sphere's diameter is roughly 60-75% of the cylinder's. It should read as a head, not as a second module of equal weight.
- The gap is roughly half the sphere's diameter. Long enough to see through clearly, short enough to look attached.
- Struts are thick enough to be at least ~2 pixels wide at gameplay zoom. Thin members will shimmer and read as noise.
- The silhouette from above is "a ball, a gap, a can", inside a black U.

## Locational damage

| Zone | Notes |
|---|---|
| Crew sphere, canopy side | The cockpit. Unshielded. |
| Crew sphere, equipment side | Life support, mounted outside the pressure hull, so exposed. Unshielded. |
| The gap | Empty. Occasionally a shot will pass straight between the cabin and the hold, hitting nothing. |
| Strut hitbox | Very small, in the gap. When hit, the cabin is shot off and the eject procedure fires immediately. Kept small because it is frustrating if it happens often. |
| Cargo cylinder | Shielded from the sides and rear only. |
| Shield generator | Covers the sides and rear. |
| Impulse engines | At the rear, where a fleeing ship takes most of its fire. |
| Turret | On top. |

The front is the most vulnerable part of the ship, and contains the most important things in it. That is deliberate. A freighter flown correctly shows its tail to the enemy, not its face. Specific effects for each zone are **open**.

## Gameplay role

- It is the starting ship.
- One jump of range is a feature. It gets new players in the habit of refuelling, and keeps them from easily getting stranded in a system without a fuel station.
- Populated space can be crossed with one jump of range just fine, but the player has to land at every system along the way. That gives new players a reason to interact with the world.
- Sluggish, lightly armed, and only partly shielded. It is meant to flee combat.

## Consistency constraints

Things that follow from the design, which missions, upgrades, and story should not contradict:

- One crew member. No passengers, so no passenger missions.
- The hold is unpressurized. Cargo that needs air or warmth - livestock, people, perishables - does not fit unless an upgrade says otherwise.
- Not capable of atmospheric flight.
- One jump of range, stock.
- There is no way to reach the cargo hold from the cabin in flight.
- Super-luminal drives are not shown and not explained, on this ship or any other.
- Neutral ships use amber lights.
- CGF is the only manufacturer.

## Open questions

- The name of the semi-neutral faction the Bourdon belongs to, and its relationship to France and to CGF.
- **Landing.** The Bourdon cannot fly in an atmosphere, but landing is central to the game, and the starting ship has to be able to do it. Orbital stations, ground-to-orbit shuttles, or some other answer need to cover this.
- What happens to the player after ejecting.
- Whether the lifeboat can manoeuvre.
- Specific effects of damage to each zone.
- Which upgrades exist, which are official CGF parts, and which are aftermarket.
- Numbers: cargo capacity, speed, acceleration, turn rate, shield and armor values, price.
- Dates.

## Visual references

- Renault 4 - the humble, utilitarian spirit of the whole ship.
- Aero Spacelines Super Guppy - the swing-open nose.
- BMW E30, North American market - the "diving board" bumper.
- Discovery One, *2001: A Space Odyssey* - spherical crew module on a spine.
- Eagle Transporter, *Space: 1999* - crew pod on an exposed frame, and how to treat the frame.
- The French Navy bathyscaphes *FNRS-3* and *Archimède* - spheres as the honest answer to pressure.
