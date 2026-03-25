import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatOptionModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { InventoryService } from './inventory.service';

@Component({
  selector: 'app-add-inventory',
  templateUrl: './add-inventory.component.html',
  styleUrls: ['./add-inventory.component.scss'],
  imports: [FormsModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatOptionModule, MatButtonModule]
})
export class AddInventoryComponent {
  private inventoryService = inject(InventoryService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);


  addInventoryForm = new FormGroup({
    item: new FormControl('', Validators.required),
    description: new FormControl('', Validators.required),
    brand: new FormControl('', Validators.required),
    color: new FormControl('', Validators.required),
    count: new FormControl('', [Validators.required, Validators.min(1)]),
    size: new FormControl('', Validators.required),
    type: new FormControl('', Validators.required),
    material: new FormControl('', Validators.required),
    quantity: new FormControl('', [Validators.required, Validators.min(0)]),
    notes: new FormControl('')
  });

  readonly addInventoryValidationMessages = {
    item: [
      { type: 'required', message: 'Item is required' }
    ],
    description: [
      { type: 'required', message: 'Description is required' }
    ],
    brand: [
      { type: 'required', message: 'Brand is required' }
    ],
    color: [
      { type: 'required', message: 'Color is required' }
    ],
    count: [
      { type: 'required', message: 'Count is required' },
      { type: 'min', message: 'Count must be a positive number' }
    ],
    size: [
      { type: 'required', message: 'Size is required' }
    ],
    type: [
      { type: 'required', message: 'Type is required' }
    ],
    material: [
      { type: 'required', message: 'Material is required' }
    ],
    quantity: [
      { type: 'required', message: 'Quantity is required' },
      { type: 'min', message: 'Quantity cannot be negative' }
    ],
    notes: [
      { type: 'required', message: 'Notes is required' }
    ]
  }


  formControlHasError(controlName: string): boolean {
    return this.addInventoryForm.get(controlName).invalid &&
      (this.addInventoryForm.get(controlName).dirty || this.addInventoryForm.get(controlName).touched);
  }

  getErrorMessage(controlName: keyof typeof this.addInventoryValidationMessages): string {
    const messages = this.addInventoryValidationMessages[controlName];
    if (!Array.isArray(messages)) {
      return ''; // either throws or ignores
    }
    for (const { type, message } of messages) {
      if (this.addInventoryForm.get(controlName)?.hasError(type)) {
        return message;
      }
    }
    return 'Unknown error';
  }

  submitForm() {
    const rawForm = this.addInventoryForm.value;
    const formData = {
      ...rawForm,
      count: rawForm.count ? parseInt(rawForm.count, 10) : null,
      quantity: rawForm.quantity ? parseInt(rawForm.quantity, 10) : null
    };
    this.inventoryService.addInventory(formData).subscribe({
      next: (newId) => {
        this.snackBar.open(
          `Added inventory item`,
          null,
          { duration: 2000 }
        );
        this.router.navigate(['/inventory', newId]);
      },
      error: err => {
        if (err.status === 400) {
          this.snackBar.open(
            `Tried to add an illegal new inventory item – Error Code: ${err.status}\nMessage: ${err.message}`,
            'OK',
            { duration: 5000 }
          );
        } else if (err.status === 500) {
          this.snackBar.open(
            `The server failed to process your request to add a new inventory item. Is the server up? – Error Code: ${err.status}\nMessage: ${err.message}`,
            'OK',
            { duration: 5000 }
          );
        } else {
          this.snackBar.open(
            `An unexpected error occurred – Error Code: ${err.status}\nMessage: ${err.message}`,
            'OK',
            { duration: 5000 }
          );
        }
      },
    });
  }

}
