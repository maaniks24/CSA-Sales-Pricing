import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { NotificationService } from '@progress/kendo-angular-notification';

@Injectable({ providedIn: 'root' })
export class Notify {
  private n = inject(NotificationService);

  success(msg: string) {
    this.show(msg, 'success');
  }

  error(err: unknown) {
    let msg = 'Something went wrong';
    if (err instanceof HttpErrorResponse) {
      msg = err.status === 0 ? 'Cannot reach the server' : err.error?.message ?? err.message;
    }
    this.show(msg, 'error', 6000);
  }

  private show(content: string, style: 'success' | 'error', hideAfter = 3000) {
    this.n.show({
      content,
      type: { style, icon: true },
      hideAfter,
      position: { horizontal: 'right', vertical: 'top' },
    });
  }
}
